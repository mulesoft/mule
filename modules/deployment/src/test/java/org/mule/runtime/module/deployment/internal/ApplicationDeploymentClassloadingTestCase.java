/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.times;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_ARTIFACTS_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.module.artifact.builder.TestArtifactDescriptor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.JarCompiler;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

/**
 * Contains test for application classloading isolation scenarios
 */
@Feature(CLASSLOADING_ISOLATION)
public class ApplicationDeploymentClassloadingTestCase extends ApplicationDeploymentTestCase {

  public ApplicationDeploymentClassloadingTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    // Only run without parallel deployment since this configuration does not affect re-deployment at all
    return asList(false);
  }

  @Test
  public void deploysAppWithPluginSharedLibrary() throws Exception {
    final ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    final ApplicationFileBuilder sharedLibPluginAppFileBuilder = appFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml")
        .dependingOn(echoPluginWithoutLib1)
        .dependingOn(callbackExtensionPlugin)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppWithPluginExportingAlreadyProvidedAppPackage() throws Exception {
    // Defines a plugin that exports org.bar which is also exported on the application
    ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = appFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml")
        .dependingOn(echoPluginWithoutLib1)
        .dependingOn(callbackExtensionPlugin)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppWithExportedPackagePrecedenceOverPlugin() throws Exception {
    // Defines a plugin that contains org.bar package, which is also exported on the application
    ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
        .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = appFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml")
        .dependingOn(echoPluginWithoutLib1)
        .dependingOn(callbackExtensionPlugin)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-17112")
  @Description("If a plugin uses a library and the application sets another version of that library as a sharedLib, the plugin internally uses its own version of the lib and not the app's. "
      + "Similar to deploysAppWithLibDifferentThanPlugin, but the bar2 dep in the app is shared in this case")
  public void pluginWithDependencyAndConflictingVersionSharedByApp() throws Exception {
    final ApplicationFileBuilder differentLibPluginAppFileBuilder = appFileBuilder("appWithLibDifferentThanPlugin")
        .definedBy("app-plugin-different-lib-config.xml")
        .dependingOn(echoPluginWithLib1)
        .dependingOn(callbackExtensionPlugin.containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class"))
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());


    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-17225")
  public void appOverridingContainerClassAlsoPluginLocal() throws Exception {
    File pluginEchoJavaxTestClassFile =
        new SingleClassCompiler().dependingOn(barUtilsJavaxJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginJavaxEcho.java"));

    ArtifactPluginFileBuilder echoPluginWithJavaxLib = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .dependingOn(new JarFileBuilder("barUtilsJavax", barUtilsJavaxJarFile))
        .containingClass(pluginEchoJavaxTestClassFile, "org/foo/echo/PluginJavaxEcho.class");

    final ApplicationFileBuilder withJavaxEchoPlugin = appFileBuilder("appWithJavaxEchoPlugin")
        .definedBy("app-with-javax-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "javax.annotation")
        .dependingOn(echoPluginWithJavaxLib)
        .dependingOn(new JarFileBuilder("barUtilsJavaxB",
                                        new JarCompiler().compiling(getResourceFile("/javax/annotation/BarUtils.java"))
                                            .compile("bar-javax-b.jar")));

    addPackedAppFromBuilder(withJavaxEchoPlugin);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, withJavaxEchoPlugin.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Description("Ensure that when a plugin2 depends on plugin1, when using something exported from plugin1, that is used")
  public void pluginDependingAndExportingFromOtherPlugin() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
        .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    // this plugin depends on a version of bar but will actually use the one from the dependant plugin
    ArtifactPluginFileBuilder echoPluginWithLib2 = new ArtifactPluginFileBuilder("echoPlugin2")
        // org.foo and org.bar are exported because plugin1 exports them.
        // Similar to how the dependency beteween http and sockets work.
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo,org.foo,org.bar")
        .dependingOn(echoPluginWithLib1)
        .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");

    final ApplicationFileBuilder usesPlugin2 = appFileBuilder("usesPlugin3")
        .definedBy("app-with-echo2-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(echoPluginWithLib2);

    addPackedAppFromBuilder(usesPlugin2);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, usesPlugin2.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Description("Similar to pluginDependingAndExportingFromOtherPlugin, but for the case when there is no dependency between plugins. When plugin2 wants to use something local that is also exported by plugin1, the dependency local to plugin2 is used")
  public void pluginNotDependingAndExportingFromOtherPlugin() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    // this plugin depends on a version of bar but will actually use the one from the dependant plugin
    ArtifactPluginFileBuilder echoPluginWithLib2 = new ArtifactPluginFileBuilder("echoPlugin2")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");

    final ApplicationFileBuilder usesPlugin2 = appFileBuilder("usesPlugin3")
        .definedBy("app-with-echo2-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(echoPluginWithLib1)
        .dependingOn(echoPluginWithLib2);

    addPackedAppFromBuilder(usesPlugin2);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, usesPlugin2.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void failsToDeployWithExtensionThatHasNonExistingIdForClassLoaderModel() throws Exception {
    String extensionName = "extension-with-classloader-model-id-non-existing";

    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId("a-non-existing-ID-describer")
        .addProperty("aProperty", "aValue").build());
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER,
                                                                    PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId(), times(1));
  }

  @Test
  public void deploysMultiPluginVersionLib() throws Exception {
    final ArtifactPluginFileBuilder echoPluginWithLib2 =
        new ArtifactPluginFileBuilder("echoPlugin2").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .dependingOn(callbackExtensionPlugin.dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
                .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class"));

    final ApplicationFileBuilder multiLibPluginAppFileBuilder = appFileBuilder("multiPluginLibVersion")
        .definedBy("multi-plugin-app-config.xml").dependingOn(echoPluginWithLib1).dependingOn(echoPluginWithLib2);

    addPackedAppFromBuilder(multiLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, multiLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysApplicationWithPluginDependingOnPlugin() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = appFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysLightApplicationWithPluginDependingOnPlugin() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .dependingOn(callbackExtensionPlugin.containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class"))
            .dependingOn(echoPlugin);

    File mavenRepoFolder = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
    File testGroupIdRepoFolder = Paths.get(mavenRepoFolder.getAbsolutePath(), "org", "mule", "test").toFile();

    JarFileBuilder testJarFileDependency = new JarFileBuilder("echoTestJar", echoTestJarFile);
    copyFile(testJarFileDependency.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.pom").toFile());
    copyFile(testJarFileDependency.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.jar").toFile());

    copyFile(echoPlugin.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0.pom").toFile());
    copyFile(echoPlugin.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0-mule-plugin.jar")
                 .toFile());

    copyCallbackExtensionFiles(testGroupIdRepoFolder);

    copyFile(dependantPlugin.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "dependantPlugin", "1.0.0", "dependantPlugin-1.0.0.pom")
                 .toFile());
    copyFile(dependantPlugin.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "dependantPlugin", "1.0.0",
                       "dependantPlugin-1.0.0-mule-plugin.jar")
                 .toFile());

    final TestArtifactDescriptor artifactFileBuilder = appFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin).usingLightWeightPackage();
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  private void copyCallbackExtensionFiles(File testGroupIdRepoFolder) throws IOException {
    copyFile(callbackExtensionPlugin.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "callbackExtensionPlugin", "1.0.0",
                       "callbackExtensionPlugin-1.0.0.pom")
                 .toFile());
    copyFile(callbackExtensionPlugin.getArtifactFile(),
             Paths
                 .get(testGroupIdRepoFolder.getAbsolutePath(), "callbackExtensionPlugin", "1.0.0",
                      "callbackExtensionPlugin-1.0.0-mule-plugin.jar")
                 .toFile());
    copyFile(callbackExtensionJarFile,
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "callbackExtension", "1.0.0", "callbackExtension-1.0.0.jar")
                 .toFile());
    copyFile(callbackExtensionPomFile,
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "callbackExtension", "1.0.0", "callbackExtension-1.0.0.pom")
                 .toFile());
  }

  @Test
  @Issue("MULE-18889")
  public void heavyApplicationServicesVisibleFromPlugin() throws Exception {
    applicationServicesVisibleFromPlugin(false);
  }

  @Test
  @Issue("MULE-18889")
  public void lightApplicationServicesVisibleFromPlugin() throws Exception {
    applicationServicesVisibleFromPlugin(true);
  }

  public void applicationServicesVisibleFromPlugin(boolean lightweight) throws Exception {
    final File spiApiJarFile =
        new JarCompiler().compiling(getResourceFile("/org/foo/spi/SpiInterface.java")).compile("spi-api.jar");
    final File spiImplJarFile =
        new JarCompiler().compiling(getResourceFile("/org/foo/spi/impl/SpiImplementation.java"))
            .including(new File(ApplicationDeploymentClassloadingTestCase.class
                .getResource("/org/foo/spi/META-INF/services/org.foo.spi.SpiInterface").toURI()),
                       "META-INF/services/org.foo.spi.SpiInterface")
            .dependingOn(spiApiJarFile)
            .compile("spi-impl.jar");

    ArtifactPluginFileBuilder spiUserPlugin =
        new ArtifactPluginFileBuilder("spiUserPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEchoSpiTestClassFile, "org/foo/echo/PluginSpiEcho.class")
            .dependingOn(echoPlugin)
            .dependingOn(new JarFileBuilder("spi-api", spiApiJarFile))
            .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.spi,org.foo.echo");

    ApplicationFileBuilder artifactFileBuilder = appFileBuilder("plugin-using-app-spi-impl")
        .definedBy("plugin-using-app-spi-impl-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(spiUserPlugin)
        .dependingOnSharedLibrary(new JarFileBuilder("spi-impl", spiImplJarFile))
        .configuredWith(EXPORTED_PACKAGES, "org.foo.spi.impl");

    if (lightweight) {
      File mavenRepoFolder = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
      File testGroupIdRepoFolder = Paths.get(mavenRepoFolder.getAbsolutePath(), "org", "mule", "test").toFile();

      JarFileBuilder spiApiJarFileDependency = new JarFileBuilder("spi-api", spiApiJarFile);
      copyFile(spiApiJarFileDependency.getArtifactPomFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spi-api", "1.0.0", "spi-api-1.0.0.pom").toFile());
      copyFile(spiApiJarFileDependency.getArtifactFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spi-api", "1.0.0", "spi-api-1.0.0.jar").toFile());

      JarFileBuilder spiImplJarFileDependency = new JarFileBuilder("spi-impl", spiImplJarFile);
      copyFile(spiImplJarFileDependency.getArtifactPomFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spi-impl", "1.0.0", "spi-impl-1.0.0.pom").toFile());
      copyFile(spiImplJarFileDependency.getArtifactFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spi-impl", "1.0.0", "spi-impl-1.0.0.jar").toFile());

      JarFileBuilder testJarFileDependency = new JarFileBuilder("echoTestJar", echoTestJarFile);
      copyFile(testJarFileDependency.getArtifactPomFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.pom").toFile());
      copyFile(testJarFileDependency.getArtifactFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.jar").toFile());

      copyFile(echoPlugin.getArtifactPomFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0.pom").toFile());
      copyFile(echoPlugin.getArtifactFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0-mule-plugin.jar")
                   .toFile());

      copyFile(spiUserPlugin.getArtifactPomFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spiUserPlugin", "1.0.0", "spiUserPlugin-1.0.0.pom").toFile());
      copyFile(spiUserPlugin.getArtifactFile(),
               Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "spiUserPlugin", "1.0.0", "spiUserPlugin-1.0.0-mule-plugin.jar")
                   .toFile());

      copyCallbackExtensionFiles(testGroupIdRepoFolder);

      artifactFileBuilder = artifactFileBuilder.usingLightWeightPackage();
    } else {
      artifactFileBuilder = artifactFileBuilder
          .configuredWith(EXPORTED_RESOURCES, "META-INF/services/org.foo.spi.SpiInterface");
    }

    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysApplicationWithPrivilegedPluginDependingOnPlugin() throws Exception {
    ArtifactPluginFileBuilder echoPlugin = new ArtifactPluginFileBuilder("echoPlugin")
        .configuredWith(PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .configuredWith(PRIVILEGED_ARTIFACTS_PROPERTY, "org.mule.test:dependantPlugin")
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile));

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = appFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void failsToDeployApplicationWithMissingPluginDependencyOnPlugin() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class");

    final TestArtifactDescriptor artifactFileBuilder = appFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/foo/EchoTest"));
    }
  }

  @Test
  public void failsToDeployApplicationWithPluginDependantOnPluginNotShipped() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = appFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, artifactFileBuilder.getId(), times(1));
  }

  @Test
  public void deploysAppWithLibDifferentThanPlugin() throws Exception {
    final ApplicationFileBuilder differentLibPluginAppFileBuilder =
        appFileBuilder("appWithLibDifferentThanPlugin").definedBy("app-plugin-different-lib-config.xml")
            .dependingOn(echoPluginWithLib1)
            .dependingOn(callbackExtensionPlugin
                .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
                .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class"));

    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingPluginResource() throws Exception {
    final ArtifactPluginFileBuilder pluginWithResource =
        new ArtifactPluginFileBuilder("resourcePlugin").configuredWith(EXPORTED_RESOURCE_PROPERTY, "/pluginResource.properties")
            .containingResource("pluginResourceSource.properties", "pluginResource.properties");

    final ApplicationFileBuilder resourcePluginAppFileBuilder = appFileBuilder("dummyWithPluginResource")
        .definedBy("plugin-resource-app-config.xml").dependingOn(pluginWithResource);

    addPackedAppFromBuilder(resourcePluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, resourcePluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppProvidingResourceForPlugin() throws Exception {
    File resourceConsumerClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/resource/ResourceConsumer.java"));

    final ArtifactPluginFileBuilder pluginUsingAppResource =
        new ArtifactPluginFileBuilder("appResourcePlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.resource")
            .containingClass(resourceConsumerClassFile, "org/foo/resource/ResourceConsumer.class");

    final TestArtifactDescriptor artifactFileBuilder =
        appFileBuilder("appProvidingResourceForPlugin")
            .definedBy("app-providing-resource-for-plugin.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(pluginUsingAppResource)
            .configuredWith(EXPORTED_RESOURCES, "META-INF/app-resource.txt")
            .usingResource(getResourceFile("/test-resource.txt").toString(), "META-INF/app-resource.txt");
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppZipWithPrivilegedExtensionPlugin() throws Exception {
    ArtifactPluginFileBuilder privilegedExtensionPlugin = createPrivilegedExtensionPlugin();

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("privilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(privilegedExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }


  @Test
  @Issue("MULE-16995")
  @Description("This test covers an scenario where an app declares extensions-api as a shared library while using a privileged extension.")
  public void appWithUnneededExtensionsApiDepDeploys() throws Exception {
    String extensionsApiLib = getProperty("extensionsApiLib");
    assumeThat(extensionsApiLib == null, is(false));

    final ApplicationFileBuilder applicationFileBuilder = appFileBuilder("privilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG)
        .dependingOn(createPrivilegedExtensionPlugin())
        .dependingOnSharedLibrary(new JarFileBuilder("mule-extensions-api", new File(extensionsApiLib))
            .withGroupId("org.mule.runtime")
            .withVersion("1.1.6"));
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void appIncludingForbiddenJavaClass() throws Exception {
    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-java-echo-plugin-config.xml")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenJavaJarFile", barUtilsForbiddenJavaJarFile))
        .dependingOn(callbackExtensionPlugin.containingClass(pluginForbiddenJavaEchoTestClassFile,
                                                             "org/foo/echo/PluginForbiddenJavaEcho.class"));

    addPackedAppFromBuilder(forbidden);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, forbidden.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("java/lang/BarUtils"));
    }
  }

  @Test
  public void appIncludingForbiddenMuleContainerClass() throws Exception {
    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-mule-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin.containingClass(pluginForbiddenMuleContainerEchoTestClassFile,
                                                             "org/foo/echo/PluginForbiddenMuleContainerEcho.class"))
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleContainerJarFile", barUtilsForbiddenMuleContainerJarFile));

    addPackedAppFromBuilder(forbidden);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, forbidden.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/mule/runtime/api/util/BarUtils"));
    }
  }

  @Test
  public void appIncludingForbiddenMuleContainerThirdParty() throws Exception {
    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-mule3rd-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin.containingClass(pluginForbiddenMuleThirdPartyEchoTestClassFile,
                                                             "org/foo/echo/PluginForbiddenMuleThirdPartyEcho.class"))
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleThirdPartyJarFile", barUtilsForbiddenMuleThirdPartyJarFile));

    addPackedAppFromBuilder(forbidden);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, forbidden.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/slf4j/BarUtils"));
    }
  }

  @Test
  public void pluginIncludingForbiddenJavaClass() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib = new ArtifactPluginFileBuilder("echoPlugin2")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .containingClass(pluginForbiddenJavaEchoTestClassFile, "org/foo/echo/PluginForbiddenJavaEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenJavaJarFile", barUtilsForbiddenJavaJarFile));

    final ApplicationFileBuilder usesPlugin2 = appFileBuilder("usesPlugin2")
        .definedBy("app-with-forbidden-java-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(echoPluginWithLib);

    addPackedAppFromBuilder(usesPlugin2);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, usesPlugin2.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("java/lang/BarUtils"));
    }
  }

  @Test
  public void pluginIncludingForbiddenMuleContainerClass() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib = new ArtifactPluginFileBuilder("echoPlugin2")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .containingClass(pluginForbiddenMuleContainerEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleContainerEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleContainerJarFile", barUtilsForbiddenMuleContainerJarFile));

    final ApplicationFileBuilder usesPlugin2 = appFileBuilder("usesPlugin2")
        .definedBy("app-with-forbidden-mule-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(echoPluginWithLib);

    addPackedAppFromBuilder(usesPlugin2);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, usesPlugin2.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/mule/runtime/api/util/BarUtils"));
    }
  }

  @Test
  public void pluginIncludingForbiddenMuleContainerThirdParty() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib = new ArtifactPluginFileBuilder("echoPlugin2")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .containingClass(pluginForbiddenMuleThirdPartyEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleThirdPartyEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleThirdPartyJarFile", barUtilsForbiddenMuleThirdPartyJarFile));

    final ApplicationFileBuilder usesPlugin2 = appFileBuilder("usesPlugin2")
        .definedBy("app-with-forbidden-mule3rd-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(echoPluginWithLib);

    addPackedAppFromBuilder(usesPlugin2);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, usesPlugin2.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/slf4j/BarUtils"));
    }
  }

  @Override
  protected Set<String> getPrivilegedArtifactIds() {
    Set<String> privilegedArtifactIds = new HashSet<>();
    privilegedArtifactIds.add(PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID);
    return privilegedArtifactIds;
  }

  private ArtifactPluginFileBuilder createPrivilegedExtensionPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(PRIVILEGED_EXTENSION_ARTIFACT_ID).setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(PRIVILEGED_EXTENSION_ARTIFACT_ID, MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.hello.PrivilegedExtension")
        .addProperty("version", "1.0");
    return new ArtifactPluginFileBuilder(PRIVILEGED_EXTENSION_ARTIFACT_ID)
        .dependingOn(new JarFileBuilder("privilegedExtensionV1", privilegedExtensionV1JarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

}
