def UPSTREAM_PROJECTS_LIST = ["Mule-runtime/mule-common/support/3.x"]

Map pipelineParams = ["upstreamProjects"   : UPSTREAM_PROJECTS_LIST.join(','),
                      "mavenTool"          : "M3",
                      "mavenAdditionalArgs": "-P distributions,release -DskipGpg  -DxDocLint='-Xdoclint:none' -DskipNoSnapshotsEnforcerPluginRule -Djava.net.preferIPv4Stack",
                      "mavenTestGoal"      : "verify -DskipIntegrationTests=false -DskipTests=false -DskipSystemTests=false -Dsurefire.rerunFailingTestsCount=5 -Dmaven.javadoc.skip -DskipVerifications",
                      "projectType"        : "Runtime" ]

runtimeBuild(pipelineParams)
