def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/master",
                               "Mule-runtime/mule-api/master",
                               "Mule-runtime/mule-extensions-api/master",
                               "Mule-runtime/mule-artifact-ast/master",
                               "Mule-runtime/data-weave/master",
                               "Mule-runtime/mule-maven-client/master" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
                       "mavenTestGoal" : "verify -Dtest='org.mule.test.module.extension.**' -DfailIfNoTests=false",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
