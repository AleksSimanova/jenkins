timeout(time: 10, unit: 'MINUTES') {

    node('gradle') {

        checkout scm

        def config = [:]
        
        stage('Read YAML') {
            script {      
                config = readYaml text: params.YAML_CONFIG
                echo "Using CONFIG: ${config}"
            }
        }

        def jobs = [:]

        def test_types = config.TESTS ?: []

        for (def test_type in test_types) {

            jobs[test_type] = node('gradle') {

                stage("Running $test_type tests") {

                        def refspecValue = env.REFSPEC ?: ""
                        def configValue = env.CONFIG ?: "default-config"

                        echo "Running tests for: ${test_type}"
                        echo "Using CONFIG: ${configValue}"
                        echo "Using REFSPEC: ${refspecValue}"

                        def parameters = [
                            string(name: 'REFSPEC', value: refspecValue),
                            string(name: 'CONFIG', value: configValue)
                        ]
                    
                    parallel (
                        "api-autotests": { -> build(job: "api-autotests", propagate: false, wait: true) },
                        "ui-autotests": { -> build(job: "ui-autotests", propagate: false, wait: true) }
                    )
                }
            }
        }
    }
}