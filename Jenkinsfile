def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/support/1.1.6",
                               "Mule-runtime/mule-api/1.1.5-NOVEMBER",
                               "Mule-runtime/mule-extensions-api/support/1.1.6",
                               "Mule-runtime/data-weave/2.1.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo 
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
