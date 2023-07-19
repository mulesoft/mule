/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * This class has the constants of the mule maven plugin used to package artifacts.
 * <p>
 * During deployment with maven packaged artifacts, the mule maven plugin coordinates are used to locate the plugin configuration
 * within the artifact pom to search for the declared shared libraries that will be exported to other plugins within the
 * application.
 * 
 * @since 4.0
 */
@NoImplement
public interface MuleMavenPlugin {

  String MULE_MAVEN_PLUGIN_GROUP_ID = "org.mule.tools.maven";
  String MULE_MAVEN_PLUGIN_ARTIFACT_ID = "mule-maven-plugin";

}
