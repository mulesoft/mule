/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.util.FileUtils.newFile;
import org.mule.runtime.module.launcher.AbstractSplashScreenTestCase;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

public class ApplicationStartedSplashScreenTestCase extends AbstractSplashScreenTestCase<ApplicationStartedSplashScreen> {

  private static final String APP_NAME = "simpleApp";
  private static final String PLUGIN_NAME = "simplePlugin";
  private static final String APP_LIB_PATH = String.format("apps/%s/lib", APP_NAME);
  private static final String MY_JAR = "myLib.jar";
  private static final String MY_ZIP = "myZip.zip";

  private ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
  private ArtifactPluginDescriptor pluginDescriptor = mock(ArtifactPluginDescriptor.class);
  private Set<ArtifactPluginDescriptor> plugins = Sets.newHashSet(pluginDescriptor);

  @BeforeClass
  public static void setUpLibrary() throws IOException {
    File libFile = newFile(workingDirectory.getRoot(), APP_LIB_PATH);
    libFile.mkdirs();
    newFile(workingDirectory.getRoot(), getAppPathFor(MY_JAR)).mkdir();
    newFile(workingDirectory.getRoot(), getAppPathFor(MY_ZIP)).mkdir();
  }

  @Before
  public void setUp() {
    splashScreen = new ApplicationStartedSplashScreen();
    when(descriptor.getName()).thenReturn(APP_NAME);
    when(descriptor.getAppProperties()).thenReturn(new HashMap<>());
    when(descriptor.getPlugins()).thenReturn(plugins);
    when(pluginDescriptor.getName()).thenReturn(PLUGIN_NAME);
  }

  @Override
  protected void setUpSplashScreen() {
    splashScreen.createMessage(descriptor);
  }

  @Override
  protected Matcher<String> getSimpleLogMatcher() {
    return is("\n**********************************************************************\n" + "* Started app '" + APP_NAME
        + "'                                            *\n"
        + "**********************************************************************");
  }

  @Override
  protected Matcher<String> getComplexLogMatcher() {
    return is("\n**********************************************************************\n" + "* Started app '" + APP_NAME
        + "'                                            *\n"
        + "* Application plugins:                                               *\n" + "*  - " + PLUGIN_NAME
        + "                                                    *\n"
        + "* Application libraries:                                             *\n" + "*  - " + MY_JAR
        + "                                                       *\n"
        + "**********************************************************************");
  }

  private static String getAppPathFor(String fileName) {
    return String.format(APP_LIB_PATH + "/%s", fileName);
  }
}
