def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/support/1.2.0",
                               "Mule-runtime/mule-api/support/1.2.0",
                               "Mule-runtime/mule-extensions-api/support/1.2.0",
                               "Mule-runtime/data-weave/master",
                               "Mule-runtime/mule-maven-client/master" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
