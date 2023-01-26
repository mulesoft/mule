/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.util;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.util.Utils.createBundleDescriptorLoader;
import static org.mule.runtime.module.deployment.internal.util.Utils.getResourceFile;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.ExtensionCompiler;
import org.mule.tck.util.CompilerUtils.JarCompiler;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

import java.io.File;
import java.net.URISyntaxException;

import org.slf4j.Logger;

public class TestArtifactsRepository {

  private static final Logger LOGGER = getLogger(TestArtifactsRepository.class);

  private static final String MIN_MULE_VERSION = "4.0.0";
  public static final String BAR_POLICY_NAME = "barPolicy";
  public static final String MULE_POLICY_CLASSIFIER = "mule-policy";
  public static final String BAZ_POLICY_NAME = "bazPolicy";
  public static final String EXCEPTION_POLICY_NAME = "exceptionPolicy";

  static {
    setUpFiles();
    setUpApplicationPluginFileBuilders();
    setUpApplicationFileBuilders();
    setUpDomainFileBuilders();
    setUpPolicyFileBuilders();
  }

  /*
   * ************************************* * Dynamically compiled classes and jars * *************************************
   */
  public static LazyTestResource<File> barUtils1ClassFile;
  public static LazyTestResource<File> barUtils1_0JarFile;
  public static LazyTestResource<File> barUtils2ClassFile;
  public static LazyTestResource<File> barUtils2_0JarFile;
  public static LazyTestResource<File> barUtilsJavaxClassFile;
  public static LazyTestResource<File> barUtilsJavaxJarFile;
  public static LazyTestResource<File> barUtilsForbiddenJavaClassFile;
  public static LazyTestResource<File> barUtilsForbiddenJavaJarFile;
  public static LazyTestResource<File> barUtilsForbiddenMuleContainerClassFile;
  public static LazyTestResource<File> barUtilsForbiddenMuleContainerJarFile;
  public static LazyTestResource<File> barUtilsForbiddenMuleThirdPartyClassFile;
  public static LazyTestResource<File> barUtilsForbiddenMuleThirdPartyJarFile;
  public static LazyTestResource<File> echoTestClassFile;
  public static LazyTestResource<File> echoTestJarFile;
  public static LazyTestResource<File> oracleExtensionJarFile;
  public static LazyTestResource<File> classloaderConnectionExtensionJarFile;
  public static LazyTestResource<File> classloaderConfigConnectionExtensionJarFile;
  public static LazyTestResource<File> defaultServiceEchoJarFile;
  public static LazyTestResource<File> defaultFooServiceJarFile;
  public static LazyTestResource<File> helloExtensionV1JarFile;
  public static LazyTestResource<File> loadClassExtensionJarFile;
  public static LazyTestResource<File> callbackExtensionJarFile;
  public static LazyTestResource<File> callbackExtensionPomFile;
  public static LazyTestResource<File> customExceptionClassFile;
  public static LazyTestResource<File> usingObjectStoreJarFile;
  public static LazyTestResource<File> goodbyeExtensionV1JarFile;
  public static LazyTestResource<File> helloExtensionV2JarFile;
  public static LazyTestResource<File> policyDependencyInjectionExtensionJarFile;
  public static LazyTestResource<File> policyConfigurationExtensionJarFile;
  public static LazyTestResource<File> loadsAppResourceCallbackClassFile;
  public static LazyTestResource<File> loadsAppResourceCallbackJarFile;
  public static LazyTestResource<File> pluginEcho1TestClassFile;
  public static LazyTestResource<File> pluginEchoSpiTestClassFile;
  public static LazyTestResource<File> pluginEcho3TestClassFile;
  public static LazyTestResource<File> pluginEcho2TestClassFile;
  public static LazyTestResource<File> pluginForbiddenJavaEchoTestClassFile;
  public static LazyTestResource<File> pluginForbiddenMuleContainerEchoTestClassFile;
  public static LazyTestResource<File> pluginForbiddenMuleThirdPartyEchoTestClassFile;
  public static LazyTestResource<File> privilegedExtensionV1JarFile;

  private static void setUpFiles() {
    barUtils1ClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/bar1/BarUtils.java"));
      }
    };

    barUtils1_0JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarFileBuilder("barUtils1",
                                  new JarCompiler().compiling(getResourceFile("/org/bar1/BarUtils.java")).compile("bar-1.0.jar"))
                                      .getArtifactFile();
      }
    };

    barUtils2ClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/bar2/BarUtils.java"));
      }
    };

    barUtils2_0JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarFileBuilder("barUtils2",
                                  new JarCompiler().compiling(getResourceFile("/org/bar2/BarUtils.java")).compile("bar-2.0.jar"))
                                      .getArtifactFile();
      }
    };

    barUtilsJavaxClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/javax/annotation/BarUtils.java"));
      }
    };

    barUtilsJavaxJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/javax/annotation/BarUtils.java")).compile("bar-javax.jar");
      }
    };

    barUtilsForbiddenJavaClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/java/lang/BarUtils.java"));
      }
    };

    barUtilsForbiddenJavaJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/java/lang/BarUtils.java")).compile("bar-javaForbidden.jar");
      }
    };

    barUtilsForbiddenMuleContainerClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/mule/runtime/api/util/BarUtils.java"));
      }
    };

    barUtilsForbiddenMuleContainerJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/org/mule/runtime/api/util/BarUtils.java"))
            .compile("bar-muleContainerForbidden.jar");
      }
    };

    barUtilsForbiddenMuleThirdPartyClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/slf4j/BarUtils.java"));
      }
    };

    barUtilsForbiddenMuleThirdPartyJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/org/slf4j/BarUtils.java"))
            .compile("bar-muleThirdPartyForbidden.jar");
      }
    };

    echoTestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/foo/EchoTest.java"));
      }
    };

    echoTestJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java")).compile("echo.jar");
      }
    };

    defaultServiceEchoJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler()
            .compiling(getResourceFile("/org/mule/echo/DefaultEchoService.java"),
                       getResourceFile("/org/mule/echo/EchoServiceProvider.java"))
            .compile("mule-module-service-echo-default-4.0-SNAPSHOT.jar");
      }
    };

    defaultFooServiceJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler()
            .compiling(getResourceFile("/org/mule/service/foo/DefaultFooService.java"),
                       getResourceFile("/org/mule/service/foo/FooServiceProvider.java"))
            .dependingOn(defaultServiceEchoJarFile.get().getAbsoluteFile())
            .compile("mule-module-service-foo-default-4.0-SNAPSHOT.jar");
      }
    };

    helloExtensionV1JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                       getResourceFile("/org/foo/hello/HelloOperation.java"))
            .including(getResourceFile("/org/foo/hello/registry-bootstrap.properties"),
                       "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
            .compile("mule-module-hello-1.0.0.jar", "1.0.0");
      }
    };

    loadClassExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/classloading/LoadClassExtension.java"),
                       getResourceFile("/org/foo/classloading/LoadClassOperation.java"))
            .including(getResourceFile("/org/foo/classloading/registry-bootstrap.properties"),
                       "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
            .compile("mule-module-classloading-1.0.0.jar", "1.0.0");
      }
    };

    callbackExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/callback/CallbackExtension.java"),
                       getResourceFile("/org/foo/callback/CallbackOperation.java"))
            .compile("mule-module-callback-1.0.0.jar", "1.0.0");
      }
    };

    callbackExtensionPomFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarFileBuilder("callbackExtension", callbackExtensionJarFile.get()).getArtifactPomFile();
      }
    };

    customExceptionClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/exception/CustomException.java"));
      }
    };

    oracleExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler().compiling(getResourceFile("/org/foo/oracle/OracleExtension.java"),
                                                 getResourceFile("/org/foo/oracle/OracleOperation.java"))
            .compile("mule-module-oracle-1.0.0.jar", "1.0.0");
      }
    };

    classloaderConnectionExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/connection/operation/ClassloaderConnectExtension.java"),
                       getResourceFile("/org/foo/connection/operation/ClassloaderOperation.java"))
            .including(getResourceFile("/org/foo/connection/extension/file.txt"), "file.txt")
            .compile("mule-module-connect-1.0.0.jar", "1.0.0");
      }
    };

    classloaderConfigConnectionExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/connection/config/ClassloaderConfigConnectExtension.java"),
                       getResourceFile("/org/foo/connection/config/ClassloaderConfigOperation.java"))
            .including(getResourceFile("/org/foo/connection/extension/file.txt"), "file.txt")
            .compile("mule-module-classloader-config-1.0.0.jar", "1.0.0");
      }
    };

    usingObjectStoreJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler().compiling(getResourceFile("/org/foo/os/UsingObjectStoreExtension.java"))
            .compile("mule-module-using-object-store-1.0.0.jar", "1.0.0");
      }
    };

    goodbyeExtensionV1JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/goodbye/GoodByeConfiguration.java"),
                       getResourceFile("/org/foo/goodbye/GoodByeExtension.java"))
            .compile("mule-module-goodbye-1.0.0.jar", "1.0.0");
      }
    };

    helloExtensionV2JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler().compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                                                 getResourceFile("/org/foo/hello/HelloOperation.java"))
            .compile("mule-module-hello-2.0.0.jar", "2.0.0");
      }
    };

    policyDependencyInjectionExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/policyIsolation/PolicyDependencyInjectionExtension.java"),
                       getResourceFile("/org/foo/policyIsolation/PolicyDependencyInjectionOperations.java"),
                       getResourceFile("/org/foo/policyIsolation/internal/InternalRegistryBean.java"),
                       getResourceFile("/org/foo/policyIsolation/internal/PolicyDependencyInjectionFunctions.java"))
            .including(getResourceFile("/org/foo/policyIsolation/registry-bootstrap.properties"),
                       "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties")
            .compile("mule-module-with-internal-dependency-4.0-SNAPSHOT.jar", "1.0.0");
      }
    };

    policyConfigurationExtensionJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/policyIsolation/PolicyConfigurationExtension.java"),
                       getResourceFile("/org/foo/policyIsolation/PolicyConfigurationOperations.java"))
            .compile("mule-module-with-internal-dependency-4.0-SNAPSHOT.jar", "1.0.0");
      }
    };

    loadsAppResourceCallbackClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceCallback.java"));
      }
    };

    loadsAppResourceCallbackJarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new JarCompiler().compiling(getResourceFile("/org/foo/LoadsAppResourceCallback.java"))
            .compile("loadsAppResourceCallback.jar");
      }
    };

    pluginEcho1TestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().dependingOn(barUtils1_0JarFile.get())
            .compile(getResourceFile("/org/foo/Plugin1Echo.java"));
      }
    };

    pluginEcho2TestClassFile = new LazyTestResource<File>() {
      @Override
      protected File doGet() throws Exception {
        return
                new SingleClassCompiler().dependingOn(barUtils2_0JarFile.get())
                        .compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));
      }
    };

    pluginEcho3TestClassFile = new LazyTestResource<File>() {
      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));
      }
    };

    pluginEchoSpiTestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().compile(getResourceFile("/org/foo/echo/PluginSpiEcho.java"));
      }
    };

    pluginForbiddenJavaEchoTestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().dependingOn(barUtilsForbiddenJavaJarFile.get())
                .compile(getResourceFile("/org/foo/echo/PluginForbiddenJavaEcho.java"));
      }
    };

    pluginForbiddenMuleContainerEchoTestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().dependingOn(barUtilsForbiddenMuleContainerJarFile.get())
                .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleContainerEcho.java"));
      }
    };

    pluginForbiddenMuleThirdPartyEchoTestClassFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new SingleClassCompiler().dependingOn(barUtilsForbiddenMuleThirdPartyJarFile.get())
                .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleThirdPartyEcho.java"));
      }
    };

    privilegedExtensionV1JarFile = new LazyTestResource<File>() {

      @Override
      protected File doGet() throws Exception {
        return new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/privileged/PrivilegedExtension.java"),
                        getResourceFile("/org/foo/privileged/PrivilegedOperation.java"))
                .compile("mule-module-privileged-1.0.jar", "1.0");
      }
    };
  }

  /*
   * ************************************* * Application plugin file builders * *************************************
   */

  public static LazyTestResource<ArtifactPluginFileBuilder> echoPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> helloExtensionV1Plugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> helloExtensionV2Plugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> goodbyeExtensionV1Plugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> oracleExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> loadClassExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> callbackExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> callbackExtensionContainingEcho2Plugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> callbackExtensionContainingForbiddenPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> callbackExtensionDependingOnBarPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> exceptionThrowingPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> byeXmlExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> moduleUsingByeXmlExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> usingObjectStorePlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> classloaderConnectExtensionPlugin;
  public static LazyTestResource<ArtifactPluginFileBuilder> classloaderConfigConnectExtensionPlugin;


  private static void setUpApplicationPluginFileBuilders() {
    echoPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return new ArtifactPluginFileBuilder("echoPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
            .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile.get()));
      }
    };

    helloExtensionV1Plugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createHelloExtensionV1PluginFileBuilder();
      }
    };

    helloExtensionV2Plugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createHelloExtensionV2PluginFileBuilder();
      }
    };

    goodbyeExtensionV1Plugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createGoodbyeExtensionV1PluginFileBuilder();
      }
    };

    oracleExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createOracleExtensionPluginFileBuilder();
      }
    };

    loadClassExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createLoadClassExtensionPluginFileBuilder();
      }
    };

    callbackExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createCallbackExtensionPluginFileBuilder();
      }
    };
    callbackExtensionContainingEcho2Plugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createCallbackExtensionPluginFileBuilder().containingClass(pluginEcho2TestClassFile.get(), "org/foo/echo/Plugin2Echo.class");
      }
    };
    callbackExtensionContainingForbiddenPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createCallbackExtensionPluginFileBuilder().containingClass(pluginForbiddenJavaEchoTestClassFile.get(),
                "org/foo/echo/PluginForbiddenJavaEcho.class");
      }
    };
    callbackExtensionDependingOnBarPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createCallbackExtensionPluginFileBuilder()
                .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile.get()))
                .containingClass(pluginEcho2TestClassFile.get(), "org/foo/echo/Plugin2Echo.class");
      }
    };


    exceptionThrowingPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createExceptionThrowingPluginFileBuilder();
      }
    };

    byeXmlExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createByeXmlPluginFileBuilder();
      }
    };

    moduleUsingByeXmlExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createModuleUsingByeXmlPluginFileBuilder();
      }
    };

    usingObjectStorePlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createUsingObjectStorePluginFileBuilder();
      }
    };

    classloaderConnectExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createClassloaderConnectExtensionPluginFileBuilder(classloaderConnectionExtensionJarFile.get(),
                                                                  "classloaderConnectExtension",
                                                                  "org.foo.connection.operation.ClassloaderConnectExtension");
      }
    };

    classloaderConfigConnectExtensionPlugin = new LazyTestResource<ArtifactPluginFileBuilder>() {

      @Override
      protected ArtifactPluginFileBuilder doGet() throws Exception {
        return createClassloaderConnectExtensionPluginFileBuilder(classloaderConfigConnectionExtensionJarFile.get(),
                                                                  "classloaderConfigConnectExtension",
                                                                  "org.foo.connection.config.ClassloaderConfigConnectExtension");
      }
    };
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
        .dependingOn(new JarFileBuilder("helloExtensionV2", helloExtensionV2JarFile.get()))
        .describedBy((mulePluginModelBuilder.build()));
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
          new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/exception/CustomException.java"));
      serviceTestClassFile = new CompilerUtils.SingleClassCompiler()
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
        .dependingOn(new JarFileBuilder("goodbyeExtensionV1", goodbyeExtensionV1JarFile.get()))
        .describedBy((mulePluginModelBuilder.build()));
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
        .dependingOn(new JarFileBuilder("usingObjectStore", usingObjectStoreJarFile.get()))
        .describedBy((mulePluginModelBuilder.build()));
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
        .dependingOn(new JarFileBuilder("helloExtensionV1", helloExtensionV1JarFile.get()))
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
        .dependingOn(new JarFileBuilder("loadClassExtension", loadClassExtensionJarFile.get()))
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
        .dependingOn(new JarFileBuilder("callbackExtension", callbackExtensionJarFile.get()))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private static ArtifactPluginFileBuilder createOracleExtensionPluginFileBuilder() {
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
        .dependingOn(new JarFileBuilder("oracleExtension", oracleExtensionJarFile.get()))
        .describedBy((mulePluginModelBuilder.build()));

    try {
      pluginFileBuilder
          .dependingOnSharedLibrary(new JarFileBuilder("oracle-driver-v1", getResourceFile("/oracle/jdbc/oracle-driver-v1.jar")));

    } catch (URISyntaxException e) {
      LOGGER.error(e.getMessage());
    }
    return pluginFileBuilder;
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
        .dependingOn(byeXmlExtensionPlugin.get())
        .describedBy(builder.build());
  }

  /*
   * ************************************* * Application file builders * *************************************
   */

  public static LazyTestResource<ApplicationFileBuilder> emptyAppFileBuilder;
  public static LazyTestResource<ApplicationFileBuilder> dummyAppDescriptorFileBuilder;
  public static LazyTestResource<ApplicationFileBuilder> dummyAppWithBrokenAstDescriptorFileBuilder;
  public static LazyTestResource<ApplicationFileBuilder> dummyFlowErrorAppDescriptorFileBuilder;
  public static LazyTestResource<ApplicationFileBuilder> dummyErrorAppOnStartDescriptorFileBuilder;

  private static void setUpApplicationFileBuilders() {
    emptyAppFileBuilder = new LazyTestResource<ApplicationFileBuilder>() {

      @Override
      protected ApplicationFileBuilder doGet() throws Exception {
        return new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
      }
    };

    dummyAppDescriptorFileBuilder = new LazyTestResource<ApplicationFileBuilder>() {

      @Override
      protected ApplicationFileBuilder doGet() throws Exception {
        return new ApplicationFileBuilder("dummy-app").definedBy("dummy-app-config.xml")
            .configuredWith("myCustomProp", "someValue")
            .containingResource("serialized/dummy-app.ast", SERIALIZED_ARTIFACT_AST_LOCATION)
            .dependingOn(callbackExtensionPlugin.get()).containingClass(echoTestClassFile.get(), "org/foo/EchoTest.class");
      }
    };

    dummyAppWithBrokenAstDescriptorFileBuilder = new LazyTestResource<ApplicationFileBuilder>() {

      @Override
      protected ApplicationFileBuilder doGet() throws Exception {
        return new ApplicationFileBuilder("dummy-app").definedBy("dummy-app-config.xml")
            .configuredWith("myCustomProp", "someValue")
            .containingResource("serialized/broken.ast", SERIALIZED_ARTIFACT_AST_LOCATION)
            .dependingOn(callbackExtensionPlugin.get()).containingClass(echoTestClassFile.get(), "org/foo/EchoTest.class");
      }
    };

    dummyFlowErrorAppDescriptorFileBuilder = new LazyTestResource<ApplicationFileBuilder>() {

      @Override
      protected ApplicationFileBuilder doGet() throws Exception {
        return new ApplicationFileBuilder("dummy-error-flow-app")
            .definedBy("dummy-app-several-flows.xml").configuredWith("myCustomProp", "someValue")
            .dependingOn(callbackExtensionPlugin.get())
            .containingClass(echoTestClassFile.get(), "org/foo/EchoTest.class");
      }
    };

    dummyErrorAppOnStartDescriptorFileBuilder = new LazyTestResource<ApplicationFileBuilder>() {

      @Override
      protected ApplicationFileBuilder doGet() throws Exception {
        return new ApplicationFileBuilder("dummy-error-app-start")
            .definedBy("dummy-error-app-start.xml").configuredWith("myCustomProp", "someValue");
      }
    };
  }

  /*
   * ************************************* * Domain file builders * *************************************
   */

  public static LazyTestResource<DomainFileBuilder> dummyDomainFileBuilder;
  public static LazyTestResource<DomainFileBuilder> exceptionThrowingPluginImportingDomain;

  private static void setUpDomainFileBuilders() {
    dummyDomainFileBuilder = new LazyTestResource<DomainFileBuilder>() {

      @Override
      protected DomainFileBuilder doGet() throws Exception {
        return new DomainFileBuilder("dummy-domain").definedBy("empty-domain-config.xml");
      }
    };

    exceptionThrowingPluginImportingDomain = new LazyTestResource<DomainFileBuilder>() {

      @Override
      protected DomainFileBuilder doGet() throws Exception {
        return new DomainFileBuilder("exception-throwing-plugin-importing-domain").definedBy("empty-domain-config.xml")
            .dependingOn(exceptionThrowingPlugin.get());
      }
    };
  }

  /*
   * ************************************* * Policy file builders * *************************************
   */

  public static LazyTestResource<PolicyFileBuilder> barPolicyFileBuilder;
  public static LazyTestResource<PolicyFileBuilder> policyUsingAppPluginFileBuilder;
  public static LazyTestResource<PolicyFileBuilder> policyIncludingPluginFileBuilder;
  public static LazyTestResource<PolicyFileBuilder> policyIncludingHelloPluginV2FileBuilder;
  public static LazyTestResource<PolicyFileBuilder> exceptionThrowingPluginImportingPolicyFileBuilder;
  public static LazyTestResource<PolicyFileBuilder> policyIncludingDependantPluginFileBuilder;

  private static void setUpPolicyFileBuilders() {
    barPolicyFileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
            .setMinMuleVersion(MIN_MULE_VERSION)
            .setName(BAR_POLICY_NAME)
            .setRequiredProduct(MULE)
            .withBundleDescriptorLoader(
                                        createBundleDescriptorLoader(BAR_POLICY_NAME,
                                                                     MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .withClassLoaderModelDescriptorLoader(
                                                  new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
            .build());
      }
    };

    policyUsingAppPluginFileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
            .setMinMuleVersion(MIN_MULE_VERSION)
            .setName(BAR_POLICY_NAME)
            .setRequiredProduct(MULE)
            .withBundleDescriptorLoader(
                                        createBundleDescriptorLoader(BAR_POLICY_NAME,
                                                                     MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .withClassLoaderModelDescriptorLoader(
                                                  new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
            .build());
      }
    };

    policyIncludingPluginFileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return createPolicyIncludingPluginFileBuilder();
      }
    };

    policyIncludingHelloPluginV2FileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return createPolicyIncludingHelloPluginV2FileBuilder();
      }
    };

    exceptionThrowingPluginImportingPolicyFileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return createExceptionThrowingPluginImportingPolicyFileBuilder();
      }
    };

    policyIncludingDependantPluginFileBuilder = new LazyTestResource<PolicyFileBuilder>() {

      @Override
      protected PolicyFileBuilder doGet() throws Exception {
        return createPolicyIncludingDependantPluginFileBuilder();
      }
    };
  }

  private static PolicyFileBuilder createPolicyIncludingHelloPluginV2FileBuilder() {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME).setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    mulePolicyModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    return new PolicyFileBuilder(BAZ_POLICY_NAME)
        .describedBy(mulePolicyModelBuilder.build())
        .dependingOn(helloExtensionV2Plugin.get());
  }

  private static PolicyFileBuilder createExceptionThrowingPluginImportingPolicyFileBuilder() {
    return new PolicyFileBuilder(EXCEPTION_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION)
        .setName(EXCEPTION_POLICY_NAME)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(
                                    createBundleDescriptorLoader(EXCEPTION_POLICY_NAME,
                                                                 MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(
                                              new MuleArtifactLoaderDescriptor(MULE_LOADER_ID,
                                                                               emptyMap()))
        .build())
        .dependingOn(callbackExtensionPlugin.get());
  }

  private static PolicyFileBuilder createPolicyIncludingPluginFileBuilder() {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build()).dependingOn(helloExtensionV1Plugin.get());
  }

  private static PolicyFileBuilder createPolicyIncludingDependantPluginFileBuilder() {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    ArtifactPluginFileBuilder dependantPlugin;
    try {
      dependantPlugin =
          new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
              .containingClass(new SingleClassCompiler().compile(getResourceFile("/org/foo/echo/Plugin3Echo.java")),
                               "org/foo/echo/Plugin3Echo.class")
              .dependingOn(helloExtensionV1Plugin.get());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build()).dependingOn(dependantPlugin);
  }

  public abstract static class LazyTestResource<T> {

    private T preCalculatedValue = null;

    public T get() throws RuntimeException {
      try {
        if (preCalculatedValue == null) {
          synchronized (this) {
            if (preCalculatedValue == null) {
              preCalculatedValue = doGet();
            }
          }
        }
        return preCalculatedValue;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    protected abstract T doGet() throws Exception;
  }
}
