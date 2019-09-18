def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/metadata-model-api/1.2.2",
                               "Mule-runtime/mule-api/1.2.2",
                               "Mule-runtime/mule-extensions-api/1.2.2",
                               "Mule-runtime/mule-maven-client/1.4.2" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                      // Comment public setting to get oldMuleArtifact 4.2.1 from private repo till we move them to the public Repo 
                      // Uncomment it after they are copied
                      // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Djava.net.preferIPv4Stack=true",
                       "mavenCompileGoal" : "clean install -U -DskipTests -DskipITs -Dinvoker.skip=true -Darchetype.test.skip -Dmaven.javadoc.skip",
]

runtimeProjectsBuild(pipelineParams)
