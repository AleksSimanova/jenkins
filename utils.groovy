def prepare_yaml_config() {

    if(!env.CONFIG){
        echo "CONFIG environment variable is not set."
        env.CONFIG = '{}'
    }
    
    def config = readYaml text: env.CONFIG

    config.each { k, v ->  
        env[k] = v.toString()
    }
}

return this