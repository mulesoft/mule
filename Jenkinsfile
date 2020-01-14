def UPSTREAM_PROJECTS_LIST = ["Mule-runtime/mule-common/3.5.x"]

Map pipelineParams = ["upstreamProjects"           : UPSTREAM_PROJECTS_LIST.join(','),
                      "jdkTool"                    : "JDK7",
                      "mavenTool"                  : "M3",
                      "enableAllureTestReportStage": false,
                      "enableSonarQubeStage"       : false,
                      "enableNexusIqStage"         : false,
                      "mavenSettingsXmlId"         : "mule-runtime-maven-settings-MuleSettings",
                      "mavenAdditionalArgs"        : "-P distributions -DskipGpg -Djava.net.preferIPv4Stack",
                      "mavenTestGoal"              : "verify -DskipIntegrationTests=false -DskipTests=false -DskipSystemTests=false -Dsurefire.rerunFailingTestsCount=5 -Dmaven.javadoc.skip -DskipVerifications",
                      "projectType"                : "Runtime" ]

runtimeBuild(pipelineParams)
