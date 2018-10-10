def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/master",
                               "Mule-runtime/mule-api/master",
                               "Mule-runtime/mule-extensions-api/master"
                               "Mule-runtime/data-weave/master" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true" ]

runtimeProjectsBuild(pipelineParams)
