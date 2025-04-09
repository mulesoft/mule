/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * This class has the constants of the mule extension plugin used to package artifacts.
 * <p>
 * During deployment with maven packaged artifacts, the mule extensions maven plugin coordinates are used to locate the plugin
 * configuration within the artifact pom to search for the declared additional dependencies that will be added to other plugins.
 *
 * @since 4.1
 */
@NoImplement
public interface MuleExtensionsMavenPlugin {

  String MULE_EXTENSIONS_PLUGIN_GROUP_ID = "org.mule.runtime.plugins";
  String MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID = "mule-extensions-maven-plugin";
}
