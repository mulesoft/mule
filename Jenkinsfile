def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/1.1.7-TESTCYCLE-2020-DRY-RUN",
                               "Mule-runtime/mule-api/1.1.6-TESTCYCLE-2020-DRY-RUN",
                               "Mule-runtime/mule-extensions-api/1.1.7-TESTCYCLE-2020-DRY-RUN",
                               "DataWeave/data-weave/support/2.1.9" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo 
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
