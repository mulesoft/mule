/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

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

  // Classes and JAR resources
  protected static File pluginEcho3TestClassFile;
  protected static File pluginEcho2TestClassFile;
  protected static File pluginForbiddenJavaEchoTestClassFile;
  protected static File pluginForbiddenMuleContainerEchoTestClassFile;
  protected static File pluginForbiddenMuleThirdPartyEchoTestClassFile;
  protected static File privilegedExtensionV1JarFile;

  // Application artifact builders
  protected ApplicationFileBuilder incompleteAppFileBuilder;
  protected ApplicationFileBuilder brokenAppFileBuilder;
  protected ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder;
  protected ApplicationFileBuilder waitAppFileBuilder;
  protected ApplicationFileBuilder dummyAppDescriptorWithPropsFileBuilder;

  // Application plugin artifact builders
  protected ArtifactPluginFileBuilder echoPluginWithLib1;

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    pluginEcho2TestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtils2_0JarFile)
            .compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));
    pluginEcho3TestClassFile = new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));

    pluginForbiddenJavaEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenJavaJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenJavaEcho.java"));
    pluginForbiddenMuleContainerEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenMuleContainerJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleContainerEcho.java"));
    pluginForbiddenMuleThirdPartyEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenMuleThirdPartyJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleThirdPartyEcho.java"));

    privilegedExtensionV1JarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/privileged/PrivilegedExtension.java"),
                                                        getResourceFile("/org/foo/privileged/PrivilegedOperation.java"))
            .compile("mule-module-privileged-1.0.jar", "1.0");
  }

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
        .containingClass(echoTestClassFile,
                         "org/foo/EchoTest.class");

    // Application plugin artifact builders
    echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
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
