def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/1.3.0-APRIL-2023",
                               "Mule-runtime/mule-api/1.3.0-APRIL-2023-WITH-W-15141905-W-15782010",
                               "Mule-runtime/mule-extensions-api/1.3.0-APRIL-2023",
                               "Mule-runtime/mule-artifact-ast/0.8.0-APRIL-2023",
                               "DataWeave/data-weave/support/2.3.0",
                               "Mule-runtime/mule-maven-client/1.5.0-APRIL-2023" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
