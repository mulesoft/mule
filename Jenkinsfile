def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/1.1.6",
                               "Mule-runtime/mule-api/1.1.5",
                               "Mule-runtime/mule-extensions-api/1.1.6",
                               "Mule-runtime/data-weave/2.1.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true" ]

runtimeProjectsBuild(pipelineParams)
