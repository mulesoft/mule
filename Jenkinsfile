def UPSTREAM_PROJECTS_LIST = ["Mule-runtime/mule-common/3.x"]

Map pipelineParams = ["upstreamProjects"   : UPSTREAM_PROJECTS_LIST.join(','),
                      "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                      "mavenAdditionalArgs": "-P distributions,release -DskipGpg  -DxDocLint='-Xdoclint:none' -DskipNoSnapshotsEnforcerPluginRule -Djava.net.preferIPv4Stack",
                      "mavenCompileGoal"   : "clean package -U -DskipTests -DskipITs -Dinvoker.skip -Dmaven.javadoc.skip",
                      "mavenTestGoal"      : "verify -DskipIntegrationTests=false -DskipTests=false -DskipSystemTests=false -Dsurefire.rerunFailingTestsCount=5 -Dmaven.javadoc.skip -DskipVerifications",
                      "mavenDeployGoal"    : "deploy -DskipTests -DskipITs -Dinvoker.skip -Dlicense.skip -DskipVerifications"]

runtimeProjectsBuild(pipelineParams)


