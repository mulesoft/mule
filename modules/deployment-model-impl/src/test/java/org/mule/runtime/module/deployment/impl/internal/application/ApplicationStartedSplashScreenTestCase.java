/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.util.FileUtils.newFile;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.AbstractSplashScreenTestCase;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

public class ApplicationStartedSplashScreenTestCase extends AbstractSplashScreenTestCase<ApplicationStartedSplashScreen> {

  private static final String APP_NAME = "simpleApp";
  private static final String PLUGIN_NAME = "simplePlugin";
  private static final String APP_LIB_PATH = String.format("%s/lib", APP_NAME);
  private static final String MY_JAR = "myLib.jar";

  private ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
  private ClassLoaderModel classLoaderModel = mock(ClassLoaderModel.class);
  private ArtifactPluginDescriptor pluginDescriptor = mock(ArtifactPluginDescriptor.class);
  private Set<ArtifactPluginDescriptor> plugins = Sets.newHashSet(pluginDescriptor);
  private static List<URL> runtimeLibs = newArrayList();

  @BeforeClass
  public static void setUpLibrary() throws IOException {
    File libFile = newFile(workingDirectory.getRoot(), APP_LIB_PATH);
    libFile.mkdirs();
    addRuntimeLibrary(MY_JAR);
  }

  private static void addRuntimeLibrary(String libraryFileName) throws MalformedURLException {
    File library = newFile(workingDirectory.getRoot(), getAppPathFor(libraryFileName));
    library.mkdir();
    runtimeLibs.add(library.toURI().toURL());
  }

  @Before
  public void setUp() {
    splashScreen = new ApplicationStartedSplashScreen();
    when(descriptor.getName()).thenReturn(APP_NAME);
    when(descriptor.getAppProperties()).thenReturn(new HashMap<>());
    when(descriptor.getPlugins()).thenReturn(plugins);
    when(pluginDescriptor.getName()).thenReturn(PLUGIN_NAME);
    when(descriptor.getClassLoaderModel()).thenReturn(classLoaderModel);

    when(classLoaderModel.getUrls()).thenReturn(runtimeLibs.toArray(new URL[0]));
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
