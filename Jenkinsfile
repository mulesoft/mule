def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/support/1.2.x",
                               "Mule-runtime/mule-api/support/1.2.x",
                               "Mule-runtime/mule-extensions-api/support/1.2.x",
                               "Mule-runtime/data-weave/2.2.x",
                               "Mule-runtime/mule-maven-client/support/1.4.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo 
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
