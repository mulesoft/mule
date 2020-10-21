def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/support/1.3.x",
                               "Mule-runtime/mule-api/1.3.0-OCTOBER",
                               "Mule-runtime/mule-extensions-api/support/1.3.x",
                               "Mule-runtime/mule-artifact-ast/support/0.8.x",
                               "Mule-runtime/data-weave/master",
                               "Mule-runtime/mule-maven-client/support/1.5.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
