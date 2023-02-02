/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.callbackExtensionPlusPlugin1Echo;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;

import java.io.File;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;

public abstract class AbstractApplicationDeploymentTestCase extends AbstractDeploymentTestCase {

  protected static final String PRIVILEGED_EXTENSION_ARTIFACT_ID = "privilegedExtensionPlugin";
  protected static final String PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID = "org.mule.test:" + PRIVILEGED_EXTENSION_ARTIFACT_ID;
  protected static final String APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG = "app-with-privileged-extension-plugin-config.xml";
  protected static final String BROKEN_CONFIG_XML = "/broken-config.xml";

  protected static final Matcher<File> exists = new BaseMatcher<File>() {

    @Override
    public boolean matches(Object o) {
      return ((File) o).exists();
    }

    @Override
    public void describeTo(org.hamcrest.Description description) {
      description.appendText("File does not exist");
    }
  };

  // Application artifact builders
  protected ApplicationFileBuilder incompleteAppFileBuilder;
  protected ApplicationFileBuilder brokenAppFileBuilder;
  protected ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder;
  protected ApplicationFileBuilder waitAppFileBuilder;
  protected ApplicationFileBuilder dummyAppDescriptorWithPropsFileBuilder;
  protected ApplicationFileBuilder dummyAppDescriptorWithStoppedFlowFileBuilder;

  public AbstractApplicationDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Before
  public void before() {
    incompleteAppFileBuilder = appFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
    brokenAppFileBuilder = appFileBuilder("broken-app").corrupted();
    brokenAppWithFunkyNameAppFileBuilder = appFileBuilder("broken-app+", brokenAppFileBuilder);
    waitAppFileBuilder = appFileBuilder("wait-app").definedBy("wait-app-config.xml");
    dummyAppDescriptorWithPropsFileBuilder = appFileBuilder("dummy-app-with-props")
        .definedBy("dummy-app-with-props-config.xml")
        .dependingOn(callbackExtensionPlusPlugin1Echo);
  }

  protected ApplicationFileBuilder appFileBuilder(final String artifactId) {
    return new ApplicationFileBuilder(artifactId);
  }

  protected ApplicationFileBuilder appFileBuilder(String artifactId, ApplicationFileBuilder source) {
    return new ApplicationFileBuilder(artifactId, source);
  }

  protected ApplicationFileBuilder appFileBuilder(String artifactId, boolean upperCaseInExtension) {
    return new ApplicationFileBuilder(artifactId, upperCaseInExtension);
  }
}
