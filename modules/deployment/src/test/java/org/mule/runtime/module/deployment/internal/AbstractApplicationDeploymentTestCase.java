/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.barUtils1_0JarFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.barUtils2_0JarFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.barUtilsForbiddenJavaJarFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.barUtilsForbiddenMuleContainerJarFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.barUtilsForbiddenMuleThirdPartyJarFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.callbackExtensionPlugin;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.echoTestClassFile;
import static org.mule.runtime.module.deployment.internal.util.TestArtifactsRepository.pluginEcho1TestClassFile;
import static org.mule.runtime.module.deployment.internal.util.Utils.getResourceFile;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

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

  // Application artifact builders
  protected ApplicationFileBuilder incompleteAppFileBuilder;
  protected ApplicationFileBuilder brokenAppFileBuilder;
  protected ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder;
  protected ApplicationFileBuilder waitAppFileBuilder;
  protected ApplicationFileBuilder dummyAppDescriptorWithPropsFileBuilder;
  protected ApplicationFileBuilder dummyAppDescriptorWithStoppedFlowFileBuilder;

  // Application plugin artifact builders
  protected ArtifactPluginFileBuilder echoPluginWithLib1;

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
        .dependingOn(callbackExtensionPlugin.get().containingClass(echoTestClassFile.get(), "org/foo/EchoTest.class"));

    // Application plugin artifact builders
    echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile.get()))
        .containingClass(pluginEcho1TestClassFile.get(), "org/foo/Plugin1Echo.class");
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
