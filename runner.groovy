node("maven") {
  timeout(300) {
      wrap([$class: 'BuildUser']) {
        currentBuild.description = "Owner: ${BUILD_USER}"

        yaml_params = readYaml text: $YAML_CONFIG

        tests_types = yaml_params['type']
      }

      stage("Checkout") {
        checkout scm
      }

      def jobs = [:]
      for test_type in tests_types {
        jobs[test_type] = {
          node('maven') {
              timeout(300) {
                  stage("Running ${test_type}") {
                      triggerJob(name: "${test_type}_tests", parameters: $YAML_CONFIG)
                  }
              }
          }
        }
      }

      parallel jobs

      stage("Publish allure report") {

        jobs.forEach({k, v ->
          sh "mkdir ./allure-results && cd ./allure-results"
          copyArtifacts filter: "**/allure-results/*", projectName: "${k}_tests", selector: specific("${v.upstreamBuild}")
        })

          allure([
            includeProperties: false,
            jdk: '',
            properties: [],
            reportBuildPolicy: 'ALWAYS',
            results: [[path: 'allure-results']]
          ])
      }

      stage("notification") {
        withCredentials([secretText(credentialsId: "bot_token", var: "TOKEN")]) {

        def testsResultData = readFile "./allure-results/export/influxDb.txt"
        testResultData = testResultData.split("\n")
        def res = [:]
        for(line in testResultData) {
            def matcher = line =~ /(?i)(passed|failed|skipped)=(\d+)/
            res[matcher[0][1]]=matcher[0][2]
        }

        def message = ""
        res.forEach({resType, number ->
          message += "$resType: $number\n"
        })

        sh "curl -X POST \
    -H 'Content-Type: application/json' \
    -d "{\"chat_id\": \"123456789\", \"text\": $message, \"disable_notification\": true}" \
    https://api.telegram.org/bot$TOKEN/sendMessage" 

        telegramSend message: message, chatId: 1111111
        }
      }



    }
}

