def UPSTREAM_PROJECTS_LIST = ["Mule-runtime/mule-common/3.5.x"]

Map pipelineParams = ["upstreamProjects"           : UPSTREAM_PROJECTS_LIST.join(','),
                      "jdkTool"                    : "JDK7",
                      "enableAllureTestReportStage": false,
                      "mavenTool"                  : "M3",
                      "mavenSettingsXmlId"         : "mule-runtime-maven-settings-MuleSettings",
                      "mavenAdditionalArgs"        : "-P distributions,release,jdk7 -DskipGpg  -DxDocLint='-Xdoclint:none' -DskipNoSnapshotsEnforcerPluginRule -Djava.net.preferIPv4Stack -T 2",
                      "mavenTestGoal"              : "verify -DskipIntegrationTests=false -DskipTests=false -DskipSystemTests=false -Dsurefire.rerunFailingTestsCount=5 -Dmaven.javadoc.skip -DskipVerifications"]

runtimeProjectsBuild(pipelineParams)
