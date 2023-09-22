/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.util.Utils.createBundleDescriptorLoader;
import static org.mule.runtime.module.deployment.internal.util.Utils.getResourceFile;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import static org.junit.Assert.fail;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.ExtensionCompiler;
import org.mule.tck.util.CompilerUtils.JarCompiler;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Utility class that holds most of the artifacts used in the deployment module test cases, in order to avoid compiling or
 * creating them repeatedly for each test.
 */
public final class TestArtifactsCatalog {

  private static final String MIN_MULE_VERSION = "4.0.0";

  static {
    try {
      initFiles();
      initArtifactPluginFileBuilders();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /*
   * Dynamically compiled classes and jars.
   */
  public static File barUtils1ClassFile;
  public static File barUtils1_0JarFile;
  public static File barUtils2ClassFile;
  public static File barUtils2_0JarFile;
  public static File barUtilsJavaxClassFile;
  public static File barUtilsJavaxJarFile;
  public static File barUtilsForbiddenJavaClassFile;
  public static File barUtilsForbiddenJavaJarFile;
  public static File barUtilsForbiddenMuleContainerClassFile;
  public static File barUtilsForbiddenMuleContainerJarFile;
  public static File barUtilsForbiddenMuleThirdPartyClassFile;
  public static File barUtilsForbiddenMuleThirdPartyJarFile;
  public static File echoTestClassFile;
  public static File echoTestJarFile;
  public static File oracleExtensionJarFile;
  public static File classloaderConnectionExtensionJarFile;
  public static File classloaderConfigConnectionExtensionJarFile;
  public static File defaulServiceEchoJarFile;
  public static File defaultFooServiceJarFile;
  public static File helloExtensionV1JarFile;
  public static File loadClassExtensionJarFile;
  public static File callbackExtensionJarFile;
  public static File callbackExtensionPomFile;
  public static File customExceptionClassFile;
  public static File usingObjectStoreJarFile;
  public static File goodbyeExtensionV1JarFile;
  private static File helloExtensionV2JarFile;
  public static File policyDependencyInjectionExtensionJarFile;
  public static File policyConfigurationExtensionJarFile;
  public static File loadsAppResourceCallbackClassFile;
  public static File loadsAppResourceCallbackJarFile;
  public static File pluginForbiddenJavaEchoTestClassFile;
  public static File pluginForbiddenMuleContainerEchoTestClassFile;
  public static File pluginForbiddenMuleThirdPartyEchoTestClassFile;
  public static File pluginEcho1ClassFile;
  public static File pluginEcho2ClassFile;
  public static File pluginEcho3ClassFile;
  public static File pluginEchoSpiTestClassFile;
  public static File privilegedExtensionV1JarFile;
  public static JarFileBuilder overriderLibrary;
  public static JarFileBuilder overrider2Library;
  public static JarFileBuilder overriderTestLibrary;
  public static File pluginEchoJavaxTestClassFile;
  public static File withLifecycleListenerExtensionJarFile;
  public static File withBrokenLifecycleListenerExtensionJarFile;

  private static void initFiles() throws URISyntaxException {
    barUtils1ClassFile = new SingleClassCompiler().compile(getResourceFile("/org/bar1/BarUtils.java"));
    barUtils1_0JarFile = new JarFileBuilder("barUtils1",
                                            new JarCompiler().compiling(getResourceFile("/org/bar1/BarUtils.java"))
                                                .compile("bar-1.0.jar"))
                                                    .getArtifactFile();

    barUtils2ClassFile = new SingleClassCompiler().compile(getResourceFile("/org/bar2/BarUtils.java"));
    barUtils2_0JarFile =
        new JarFileBuilder("barUtils2",
                           new JarCompiler().compiling(getResourceFile("/org/bar2/BarUtils.java")).compile("bar-2.0.jar"))
                               .getArtifactFile();

    barUtilsJavaxClassFile = new SingleClassCompiler().compile(getResourceFile("/packagetesting/javax/annotation/BarUtils.java"));
    barUtilsJavaxJarFile =
        new JarCompiler().compiling(getResourceFile("/packagetesting/javax/annotation/BarUtils.java")).compile("bar-javax.jar");

    barUtilsForbiddenJavaClassFile =
        new SingleClassCompiler().compile(getResourceFile("/packagetesting/java/lang/BarUtils.java"));
    barUtilsForbiddenJavaJarFile =
        new JarCompiler().compiling(getResourceFile("/packagetesting/java/lang/BarUtils.java")).compile("bar-javaForbidden.jar");

    barUtilsForbiddenMuleContainerClassFile =
        new SingleClassCompiler().compile(getResourceFile("/packagetesting/org/mule/runtime/api/util/BarUtils.java"));
    barUtilsForbiddenMuleContainerJarFile =
        new JarCompiler().compiling(getResourceFile("/packagetesting/org/mule/runtime/api/util/BarUtils.java"))
            .compile("bar-muleContainerForbidden.jar");

    barUtilsForbiddenMuleThirdPartyClassFile =
        new SingleClassCompiler().compile(getResourceFile("/packagetesting/org/slf4j/BarUtils.java"));
    barUtilsForbiddenMuleThirdPartyJarFile =
        new JarCompiler().compiling(getResourceFile("/packagetesting/org/slf4j/BarUtils.java"))
            .compile("bar-muleThirdPartyForbidden.jar");

    echoTestClassFile = new SingleClassCompiler().compile(getResourceFile("/org/foo/EchoTest.java"));
    echoTestJarFile = new JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java")).compile("echo.jar");

    defaulServiceEchoJarFile = new JarCompiler()
        .compiling(getResourceFile("/packagetesting/org/mule/echo/DefaultEchoService.java"),
                   getResourceFile("/packagetesting/org/mule/echo/EchoServiceProvider.java"))
        .including(getResourceFile("/packagetesting/org/mule/echo/MANIFEST.MF"),
                   "META-INF/MANIFEST.MF")
        .compile("mule-module-service-echo-4.0-SNAPSHOT.jar");

    defaultFooServiceJarFile = new JarCompiler()
        .compiling(getResourceFile("/packagetesting/org/mule/service/foo/DefaultFooService.java"),
                   getResourceFile("/packagetesting/org/mule/service/foo/FooServiceProvider.java"))
        .dependingOn(defaulServiceEchoJarFile.getAbsoluteFile())
        .including(getResourceFile("/packagetesting/org/mule/service/foo/MANIFEST.MF"),
                   "META-INF/MANIFEST.MF")
        .compile("mule-module-service-foo-4.0-SNAPSHOT.jar");

    helloExtensionV1JarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                   getResourceFile("/org/foo/hello/HelloOperation.java"))
        .including(getResourceFile("/org/foo/hello/registry-bootstrap.properties"),
                   "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
        .compile("mule-module-hello-1.0.0.jar", "1.0.0");

    loadClassExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/classloading/LoadClassExtension.java"),
                   getResourceFile("/org/foo/classloading/LoadClassOperation.java"))
        .including(getResourceFile("/org/foo/classloading/registry-bootstrap.properties"),
                   "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
        .compile("mule-module-classloading-1.0.0.jar", "1.0.0");

    callbackExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/callback/CallbackExtension.java"),
                   getResourceFile("/org/foo/callback/CallbackOperation.java"))
        .compile("mule-module-callback-1.0.0.jar", "1.0.0");
    callbackExtensionPomFile = new JarFileBuilder("callbackExtension", callbackExtensionJarFile)
        .getArtifactPomFile();
    customExceptionClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/exception/CustomException.java"));

    oracleExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/oracle/OracleExtension.java"),
                   getResourceFile("/org/foo/oracle/OracleOperation.java"))
        .compile("mule-module-oracle-1.0.0.jar", "1.0.0");

    classloaderConnectionExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/connection/operation/ClassloaderConnectExtension.java"),
                   getResourceFile("/org/foo/connection/operation/ClassloaderOperation.java"))
        .including(getResourceFile("/org/foo/connection/extension/file.txt"),
                   "file.txt")
        .compile("mule-module-connect-1.0.0.jar", "1.0.0");

    classloaderConfigConnectionExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/connection/config/ClassloaderConfigConnectExtension.java"),
                   getResourceFile("/org/foo/connection/config/ClassloaderConfigOperation.java"))
        .including(getResourceFile("/org/foo/connection/extension/file.txt"),
                   "file.txt")
        .compile("mule-module-classloader-config-1.0.0.jar", "1.0.0");

    usingObjectStoreJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/os/UsingObjectStoreExtension.java"))
        .compile("mule-module-using-object-store-1.0.0.jar", "1.0.0");

    goodbyeExtensionV1JarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/goodbye/GoodByeConfiguration.java"),
                   getResourceFile("/org/foo/goodbye/GoodByeExtension.java"))
        .compile("mule-module-goodbye-1.0.0.jar", "1.0.0");

    helloExtensionV2JarFile = new ExtensionCompiler().compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                                                                getResourceFile("/org/foo/hello/HelloOperation.java"))
        .compile("mule-module-hello-2.0.0.jar", "2.0.0");

    policyDependencyInjectionExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/policyIsolation/PolicyDependencyInjectionExtension.java"),
                   getResourceFile("/org/foo/policyIsolation/PolicyDependencyInjectionOperations.java"),
                   getResourceFile("/org/foo/policyIsolation/internal/InternalRegistryBean.java"),
                   getResourceFile("/org/foo/policyIsolation/internal/PolicyDependencyInjectionFunctions.java"))
        .including(getResourceFile("/org/foo/policyIsolation/registry-bootstrap.properties"),
                   "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
        .compile("mule-module-with-internal-dependency-4.0-SNAPSHOT.jar", "1.0.0");

    policyConfigurationExtensionJarFile = new ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/policyIsolation/PolicyConfigurationExtension.java"),
                   getResourceFile("/org/foo/policyIsolation/PolicyConfigurationOperations.java"))
        .compile("mule-module-with-internal-dependency-4.0-SNAPSHOT.jar", "1.0.0");

    loadsAppResourceCallbackClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceCallback.java"));
    loadsAppResourceCallbackJarFile = new JarCompiler().compiling(getResourceFile("/org/foo/LoadsAppResourceCallback.java"))
        .compile("loadsAppResourceCallback.jar");

    pluginEcho1ClassFile = new SingleClassCompiler().dependingOn(barUtils1_0JarFile)
        .compile(getResourceFile("/org/foo/Plugin1Echo.java"));
    pluginEcho2ClassFile = new SingleClassCompiler().dependingOn(barUtils2_0JarFile)
        .compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));
    pluginEcho3ClassFile = new SingleClassCompiler()
        .compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));

    pluginForbiddenJavaEchoTestClassFile = new SingleClassCompiler().dependingOn(barUtilsForbiddenJavaJarFile)
        .compile(getResourceFile("/org/foo/echo/PluginForbiddenJavaEcho.java"));
    pluginForbiddenMuleContainerEchoTestClassFile = new SingleClassCompiler().dependingOn(barUtilsForbiddenMuleContainerJarFile)
        .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleContainerEcho.java"));
    pluginForbiddenMuleThirdPartyEchoTestClassFile = new SingleClassCompiler().dependingOn(barUtilsForbiddenMuleThirdPartyJarFile)
        .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleThirdPartyEcho.java"));

    pluginEchoSpiTestClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/echo/PluginSpiEcho.java"));

    privilegedExtensionV1JarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/privileged/PrivilegedExtension.java"),
                                                        getResourceFile("/org/foo/privileged/PrivilegedOperation.java"))
            .compile("mule-module-privileged-1.0.jar", "1.0");

    overriderLibrary = new JarFileBuilder("overrider-library", new JarCompiler()
        .compiling(getResourceFile("/classloading-troubleshooting/src/OverrideMe.java"))
        .compile("overrider-library.jar"));
    overrider2Library = new JarFileBuilder("overrider2-library", new JarCompiler()
        .compiling(getResourceFile("/classloading-troubleshooting/src/OverrideMe2.java"))
        .compile("overrider2-library.jar"));
    overriderTestLibrary = new JarFileBuilder("overrider-test-library", new JarCompiler()
        .compiling(getResourceFile("/classloading-troubleshooting/src/test/OverrideMe.java"))
        .compile("overrider-test-library.jar"));

    withLifecycleListenerExtensionJarFile = new CompilerUtils.ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/withLifecycleListener/WithLifecycleListenerExtension.java"),
                   getResourceFile("/org/foo/withLifecycleListener/WithLifecycleListenerOperation.java"),
                   getResourceFile("/org/foo/withLifecycleListener/LeakedThread.java"),
                   getResourceFile("/org/foo/withLifecycleListener/LifecycleListener.java"))
        .compile("mule-extension-with-lifecycle-listener-1.0-SNAPSHOT.jar", "1.0.0");

    withBrokenLifecycleListenerExtensionJarFile = new CompilerUtils.ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/withBrokenLifecycleListener/WithBrokenLifecycleListenerExtension.java"),
                   getResourceFile("/org/foo/withBrokenLifecycleListener/LifecycleListener.java"))
        .compile("mule-extension-with-broken-lifecycle-listener-1.0-SNAPSHOT.jar", "1.0.0");

    pluginEchoJavaxTestClassFile = new SingleClassCompiler()
        .dependingOn(barUtilsJavaxJarFile)
        .compile(getResourceFile("/org/foo/echo/PluginJavaxEcho.java"));
  }


  /*
   * Application plugin file builders.
   */
  public static ArtifactPluginFileBuilder echoPlugin;
  public static ArtifactPluginFileBuilder helloExtensionV1Plugin;
  public static ArtifactPluginFileBuilder helloExtensionV2Plugin;
  public static ArtifactPluginFileBuilder goodbyeExtensionV1Plugin;
  public static ArtifactPluginFileBuilder oracleExtensionPlugin;
  public static ArtifactPluginFileBuilder loadClassExtensionPlugin;
  public static ArtifactPluginFileBuilder callbackExtensionPlugin;
  public static ArtifactPluginFileBuilder callbackExtensionPlusEcho;
  public static ArtifactPluginFileBuilder callbackExtensionPlusPlugin1Echo;
  public static ArtifactPluginFileBuilder callbackExtensionPlusPlugin2Echo;
  public static ArtifactPluginFileBuilder callbackExtensionPlusPlugin2EchoAndBar2;
  public static ArtifactPluginFileBuilder callbackExtensionPlusPlugin3Echo;
  public static ArtifactPluginFileBuilder callbackExtensionPlusForbidden;
  public static ArtifactPluginFileBuilder callbackExtensionPlusForbiddenContainerClass;
  public static ArtifactPluginFileBuilder callbackExtensionPlusForbiddenThirdPartyClass;
  public static ArtifactPluginFileBuilder callbackExtensionLoadingResource;
  public static ArtifactPluginFileBuilder callbackExtensionCustomException;
  public static ArtifactPluginFileBuilder exceptionThrowingPlugin;
  public static ArtifactPluginFileBuilder byeXmlExtensionPlugin;
  public static ArtifactPluginFileBuilder moduleUsingByeXmlExtensionPlugin;
  public static ArtifactPluginFileBuilder usingObjectStorePlugin;
  public static ArtifactPluginFileBuilder classloaderConnectExtensionPlugin;
  public static ArtifactPluginFileBuilder classloaderConfigConnectExtensionPlugin;
  public static ArtifactPluginFileBuilder echoPluginWithLib1;
  public static ArtifactPluginFileBuilder echoPluginWithJavaxLib;
  public static ArtifactPluginFileBuilder withLifecycleListenerPlugin;
  public static ArtifactPluginFileBuilder withBrokenLifecycleListenerPlugin;

  public static void initArtifactPluginFileBuilders() throws URISyntaxException {
    echoPlugin = createEchoPluginBuilder();
    helloExtensionV1Plugin = createHelloExtensionV1PluginFileBuilder();
    helloExtensionV2Plugin = createHelloExtensionV2PluginFileBuilder();
    goodbyeExtensionV1Plugin = createGoodbyeExtensionV1PluginFileBuilder();
    oracleExtensionPlugin = createOracleExtensionPluginFileBuilder();
    loadClassExtensionPlugin = createLoadClassExtensionPluginFileBuilder();
    exceptionThrowingPlugin = createExceptionThrowingPluginFileBuilder();
    byeXmlExtensionPlugin = createByeXmlPluginFileBuilder();
    moduleUsingByeXmlExtensionPlugin = createModuleUsingByeXmlPluginFileBuilder();
    usingObjectStorePlugin = createUsingObjectStorePluginFileBuilder();
    classloaderConnectExtensionPlugin =
        createClassloaderConnectExtensionPluginFileBuilder(classloaderConnectionExtensionJarFile, "classloaderConnectExtension",
                                                           "org.foo.connection.operation.ClassloaderConnectExtension");
    classloaderConfigConnectExtensionPlugin =
        createClassloaderConnectExtensionPluginFileBuilder(classloaderConfigConnectionExtensionJarFile,
                                                           "classloaderConfigConnectExtension",
                                                           "org.foo.connection.config.ClassloaderConfigConnectExtension");
    echoPluginWithLib1 = createEchoPluginWithLib1();

    callbackExtensionPlugin = createCallbackExtensionPluginFileBuilder();
    callbackExtensionPlusEcho = createCallbackExtensionPluginFileBuilder()
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
    callbackExtensionPlusPlugin1Echo = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class");
    callbackExtensionPlusPlugin2Echo = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginEcho2ClassFile, "org/foo/echo/Plugin2Echo.class");
    callbackExtensionPlusPlugin2EchoAndBar2 = createCallbackExtensionPluginFileBuilder()
        .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .containingClass(pluginEcho2ClassFile, "org/foo/echo/Plugin2Echo.class");
    callbackExtensionPlusPlugin3Echo = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginEcho3ClassFile, "org/foo/echo/Plugin3Echo.class");
    callbackExtensionPlusForbidden = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginForbiddenJavaEchoTestClassFile, "org/foo/echo/PluginForbiddenJavaEcho.class");
    callbackExtensionPlusForbiddenContainerClass = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginForbiddenMuleContainerEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleContainerEcho.class");
    callbackExtensionPlusForbiddenThirdPartyClass = createCallbackExtensionPluginFileBuilder()
        .containingClass(pluginForbiddenMuleThirdPartyEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleThirdPartyEcho.class");
    callbackExtensionLoadingResource = createCallbackExtensionPluginFileBuilder()
        .containingClass(loadsAppResourceCallbackClassFile, "org/foo/LoadsAppResourceCallback.class");
    callbackExtensionCustomException = createCallbackExtensionPluginFileBuilder()
        .containingClass(customExceptionClassFile, "org/exception/CustomException.class");

    echoPluginWithJavaxLib = createEchoPluginWithJavaxLib();
    withLifecycleListenerPlugin = createWithLifecycleListenerPlugin();
    withBrokenLifecycleListenerPlugin = createWithBrokenLifecycleListenerPlugin();
  }

  private static ArtifactPluginFileBuilder createEchoPluginWithJavaxLib() {
    return new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .dependingOn(new JarFileBuilder("barUtilsJavax", barUtilsJavaxJarFile))
        .containingClass(pluginEchoJavaxTestClassFile, "org/foo/echo/PluginJavaxEcho.class");
  }

  private static ArtifactPluginFileBuilder createEchoPluginWithLib1() {
    return new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class");
  }

  private static ArtifactPluginFileBuilder createEchoPluginBuilder() {
    return new ArtifactPluginFileBuilder("echoPlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile));
  }

  private static ArtifactPluginFileBuilder createByeXmlPluginFileBuilder() {
    final String prefixModuleName = "module-bye";
    String extensionName = "bye-extension";
    final String resources = "org/mule/module/";
    String moduleDestination = resources + prefixModuleName + ".xml";
    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER, MULE_LOADER_ID));
    builder.setRequiredProduct(MULE).setMinMuleVersion(MIN_MULE_VERSION);

    return new ArtifactPluginFileBuilder(extensionName)
        .containingResource("module-byeSource.xml", moduleDestination)
        .containingResource("module-using-bye-catalogSource.xml", resources + prefixModuleName + "-catalog.xml")
        .containingResource("module-bye-type-schemaSource.json", resources + "type1-schema.json")
        .containingResource("module-bye-type-schemaSource.json", resources + "inner/folder/type2-schema.json")
        .containingResource("module-bye-type-schemaSource.json", "org/mule/type3-schema.json")
        .describedBy(builder.build());
  }

  private static ArtifactPluginFileBuilder createModuleUsingByeXmlPluginFileBuilder() {
    String moduleFileName = "module-using-bye.xml";
    String extensionName = "using-bye-extension";
    String moduleDestination = "org/mule/module/" + moduleFileName;

    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION)
            .setRequiredProduct(MULE);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .addProperty(EXPORTED_PACKAGES, singletonList("org.foo")).setId(MULE_LOADER_ID)
        .build());
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER, MULE_LOADER_ID));

    return new ArtifactPluginFileBuilder(extensionName)
        .containingResource("module-using-byeSource.xml", moduleDestination)
        .dependingOn(byeXmlExtensionPlugin)
        .describedBy(builder.build());
  }

  private static ArtifactPluginFileBuilder createExceptionThrowingPluginFileBuilder() {
    final String pluginName = "exceptionPlugin";

    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION)
        .setName(pluginName)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(pluginName,
                                                                 MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID,
                                                                 "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID)
        .addProperty(EXPORTED_RESOURCES,
                     asList("/META-INF/mule-exception.xsd",
                            "/META-INF/mule.schemas"))
        .build());

    File exceptionTestClassFile = null;
    File serviceTestClassFile = null;

    try {
      exceptionTestClassFile =
          new SingleClassCompiler().compile(getResourceFile("/org/exception/CustomException.java"));
      serviceTestClassFile = new SingleClassCompiler()
          .compile(getResourceFile("/org/exception/ExceptionComponentBuildingDefinitionProvider.java"));
    } catch (URISyntaxException e) {
      fail(e.getMessage());
    }

    return new ArtifactPluginFileBuilder("exceptionPlugin")
        .containingResource("exception/META-INF/mule.schemas", "META-INF/mule.schemas")
        .containingResource("exception/META-INF/mule-exception.xsd", "META-INF/mule-exception.xsd")
        .containingResource("exception/META-INF/services/org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider",
                            "META-INF/services/org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider")
        .containingClass(exceptionTestClassFile, "org/exception/CustomException.class")
        .containingClass(serviceTestClassFile, "org/exception/ExceptionComponentBuildingDefinitionProvider.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, "META-INF/mule-exception.xsd,META-INF/mule.schemas")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.exception")
        .describedBy(mulePluginModelBuilder.build());

  }

  private static ArtifactPluginFileBuilder createHelloExtensionV1PluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.hello.HelloExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("helloExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("helloExtensionV1", helloExtensionV1JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createLoadClassExtensionPluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("loadClassExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("loadClassExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.classloading.LoadClassExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("loadClassExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("loadClassExtension", loadClassExtensionJarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createCallbackExtensionPluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("callbackExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("callbackExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.callback.CallbackExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("callbackExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("callbackExtension", callbackExtensionJarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createOracleExtensionPluginFileBuilder() throws URISyntaxException {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("oracleExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("oracleExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.oracle.OracleExtension")
        .addProperty("version", "1.0.0");
    ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("oracleExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("oracleExtension", oracleExtensionJarFile))
        .describedBy((mulePluginModelBuilder.build()));

    pluginFileBuilder
        .dependingOnSharedLibrary(new JarFileBuilder("oracle-driver-v1", getResourceFile("/oracle/jdbc/oracle-driver-v1.jar")));

    return pluginFileBuilder;
  }

  private static ArtifactPluginFileBuilder createClassloaderConnectExtensionPluginFileBuilder(File jarFile, String extensionName,
                                                                                              String extensionPath) {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(extensionName + "Plugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName + "Plugin",
                                                                 MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", extensionPath)
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder(extensionName + "Plugin-1.0.0")
        .dependingOn(new JarFileBuilder(extensionName, jarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createUsingObjectStorePluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("usingObjectStorePlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("usingObjectStorePlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.os.UsingObjectStoreExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("usingObjectStorePlugin-1.0.0")
        .dependingOn(new JarFileBuilder("usingObjectStore", usingObjectStoreJarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createHelloExtensionV2PluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "2.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID).build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.hello.HelloExtension")
        .addProperty("version", "2.0.0");
    return new ArtifactPluginFileBuilder("helloExtensionPlugin-2.0.0")
        .dependingOn(new JarFileBuilder("helloExtensionV2", helloExtensionV2JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createGoodbyeExtensionV1PluginFileBuilder() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("goodbyeExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("goodbyeExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "2.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID).build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.goodbye.GoodByeExtension")
        .addProperty("version", "2.0.0");
    return new ArtifactPluginFileBuilder("goodbyeExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("goodbyeExtensionV1", goodbyeExtensionV1JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }


  private static ArtifactPluginFileBuilder createWithLifecycleListenerPlugin() {
    return createPluginBuilder("withLifecycleListenerPlugin",
                               "org.foo.withLifecycleListener.WithLifecycleListenerExtension",
                               withLifecycleListenerExtensionJarFile);
  }

  private static ArtifactPluginFileBuilder createWithBrokenLifecycleListenerPlugin() {
    return createPluginBuilder("withBrokenLifecycleListenerPlugin",
                               "org.foo.withBrokenLifecycleListener.WithBrokenLifecycleListenerExtension",
                               withBrokenLifecycleListenerExtensionJarFile);
  }

  private static ArtifactPluginFileBuilder createPluginBuilder(String artifactId, String extensionModelType, File jarFile) {
    String version = "1.0.0";
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION)
        .setName(artifactId)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(artifactId, MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, version));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", extensionModelType)
        .addProperty("version", version);
    return new ArtifactPluginFileBuilder(artifactId)
        .dependingOn(new JarFileBuilder(artifactId, jarFile))
        .describedBy(mulePluginModelBuilder.build());
  }
}
