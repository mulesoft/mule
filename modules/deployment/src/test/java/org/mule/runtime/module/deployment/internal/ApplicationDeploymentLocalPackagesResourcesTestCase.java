/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Arrays.asList;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ApplicationDeploymentLocalPackagesResourcesTestCase extends AbstractApplicationDeploymentTestCase {

  @Parameterized.Parameters(name = "Parallel: {0}; classloaderModelVersion: {1}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false, "1.0.0"},
        {false, "1.1.0"},
        {false, "1.2.0"},
    });
  }

  private String classloaderModelVersion;

  public ApplicationDeploymentLocalPackagesResourcesTestCase(boolean parallelDeployment, String classloaderModelVersion) {
    super(parallelDeployment);
    this.classloaderModelVersion = classloaderModelVersion;
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form plugin's Processor cannot access internal resources/packages of the application")
  public void deploysAppWithLocalPackageAndPlugin() throws Exception {
    ArtifactPluginFileBuilder loadsAppResourcePlugin = new ArtifactPluginFileBuilder("loadsAppResourcePlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(loadsAppResourceCallbackClassFile, "org/foo/LoadsAppResourceCallback.class");

    ApplicationFileBuilder nonExposingAppFileBuilder = appFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-loads-app-resource-plugin-config.xml")
        .containingClass(barUtils1ClassFile, "org/bar/BarUtils.class")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt")
        .dependingOn(loadsAppResourcePlugin);

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form application's Processor can access internal resources/packages of the application")
  public void deploysAppWithLocalPackage() throws Exception {
    ApplicationFileBuilder nonExposingAppFileBuilder = appFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-loads-app-resource-plugin-config.xml")
        .containingClass(loadsAppResourceCallbackClassFile, "org/foo/LoadsAppResourceCallback.class")
        .containingClass(barUtils1ClassFile, "org/bar/BarUtils.class")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt");

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form plugin's ProcessorInterceptor cannot access internal resources/packages of the application")
  public void deploysAppWithLocalPackageAndPluginWithInterceptors() throws Exception {
    File loadsAppResourceInterceptorFactoryClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceInterceptorFactory.java"));
    File loadsAppResourceInterceptorClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceInterceptor.java"));

    ArtifactPluginFileBuilder loadsAppResourceInterceptorPlugin =
        new ArtifactPluginFileBuilder("loadsAppResourceInterceptorPlugin")
            .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.lalala")
            .containingClass(loadsAppResourceInterceptorFactoryClassFile, "org/foo/LoadsAppResourceInterceptorFactory.class")
            .containingClass(loadsAppResourceInterceptorClassFile, "org/foo/LoadsAppResourceInterceptor.class")
            .containingResource("registry-bootstrap-loads-app-resource-pif.properties",
                                "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties");

    ApplicationFileBuilder nonExposingAppFileBuilder = appFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-plugin-bootstrap.xml")
        .containingClass(barUtils1ClassFile, "org/bar/BarUtils.class")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt")
        .dependingOn(loadsAppResourceInterceptorPlugin);

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form application's ProcessorInterceptor can access internal resources/packages of the application")
  public void deploysAppWithInterceptorsAndLocalPackage() throws Exception {
    File loadsOwnResourceInterceptorFactoryClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsOwnResourceInterceptorFactory.java"));
    File loadsOwnResourceInterceptorClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsOwnResourceInterceptor.java"));

    ApplicationFileBuilder nonExposingAppFileBuilder = appFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-interceptor.xml")
        .containingClass(loadsOwnResourceInterceptorFactoryClassFile, "org/foo/LoadsOwnResourceInterceptorFactory.class")
        .containingClass(loadsOwnResourceInterceptorClassFile, "org/foo/LoadsOwnResourceInterceptor.class")
        .containingClass(barUtils1ClassFile, "org/bar/BarUtils.class")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt");

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  protected ApplicationFileBuilder appFileBuilder(final String artifactId) {
    return new ApplicationFileBuilder(artifactId)
        .withClassloaderModelVersion(classloaderModelVersion);
  }

}
