/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.test.allure.AllureConstants.SplashScreenFeature.SPLASH_SCREEN;

import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.deployment.impl.internal.base.AbstractSplashScreenTestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import io.qameta.allure.Feature;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

@Feature(SPLASH_SCREEN)
public class ApplicationStartedSplashScreenTestCase extends AbstractSplashScreenTestCase<ApplicationStartedSplashScreen> {

  private static final String APP_NAME = "simpleApp";
  private static final String PLUGIN_NAME = "simplePlugin";
  private static final String APP_LIB_PATH = String.format("%s/lib", APP_NAME);
  private static final String MY_JAR = "myLib.jar";
  public static final String PLUGIN_GROUP_ID = "org.mule.tests";
  public static final String PLUGIN_ARTIFACT_ID = "simple-plugin";
  public static final String PLUGIN_VERSION = "1.0.0";

  private ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
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
    runtimeLibs.add(workingDirectory.getRoot().toURL());
    runtimeLibs.add(library.toURI().toURL());
  }

  @Before
  public void setUp() {
    splashScreen = new ApplicationStartedSplashScreen();
    ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder = new ClassLoaderConfigurationBuilder();
    runtimeLibs.stream().forEach(classLoaderConfigurationBuilder::containing);
    ClassLoaderConfiguration classLoaderConfiguration = classLoaderConfigurationBuilder.build();

    ArtifactPluginDescriptor pluginDescriptor = new ArtifactPluginDescriptor(PLUGIN_NAME);
    pluginDescriptor.setBundleDescriptor(new BundleDescriptor.Builder()
        .setGroupId(PLUGIN_GROUP_ID)
        .setArtifactId(PLUGIN_ARTIFACT_ID)
        .setVersion(PLUGIN_VERSION)
        .build());

    when(descriptor.getName()).thenReturn(APP_NAME);
    when(descriptor.getAppProperties()).thenReturn(new HashMap<>());
    when(descriptor.getPlugins()).thenReturn(newHashSet(pluginDescriptor));
    when(descriptor.getClassLoaderConfiguration()).thenReturn(classLoaderConfiguration);
  }

  @Override
  protected void setUpSplashScreen() {
    splashScreen.createMessage(descriptor);
  }

  @Override
  protected Matcher<String> getSimpleLogMatcher() {
    return is(equalToIgnoringLineBreaks("\n**********************************************************************\n"
        + "* Started app '" + APP_NAME
        + "'                                            *\n"
        + "**********************************************************************"));
  }

  @Override
  protected Matcher<String> getComplexLogMatcher() {
    return is(equalToIgnoringLineBreaks("\n**********************************************************************\n"
        + "* Started app '" + APP_NAME
        + "'                                            *\n"
        + "* Application plugins:                                               *\n" + "*  - " + PLUGIN_NAME + " : "
        + PLUGIN_VERSION
        + "                                            *\n"
        + "* Application libraries:                                             *\n" + "*  - " + MY_JAR
        + "                                                       *\n"
        + "**********************************************************************"));
  }

  private static String getAppPathFor(String fileName) {
    return String.format(APP_LIB_PATH + "/%s", fileName);
  }
}
