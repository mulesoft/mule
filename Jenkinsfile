def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/support/1.1.x",
                               "Mule-runtime/mule-api/support/1.1.6",
                               "Mule-runtime/mule-extensions-api/support/1.1.7",
                               "Mule-runtime/data-weave/support/2.1.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
