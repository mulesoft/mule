/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildSchedulerServiceFile;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppConfigFolderPath;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_ARTIFACTS_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.PROPERTY_DOMAIN;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.VERSION;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.deployment.internal.DeploymentServiceTestCase.TestPolicyProcessor.invocationCount;
import static org.mule.runtime.module.deployment.internal.DeploymentServiceTestCase.TestPolicyProcessor.policyParametrization;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.PARALLEL_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.TestApplicationFactory.createTestApplicationFactory;
import static org.mule.runtime.module.service.ServiceDescriptorFactory.SERVICE_PROVIDER_CLASS_NAME;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.TEST_MESSAGE;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel.MulePluginModelBuilder;
import org.mule.runtime.api.deployment.meta.MulePolicyModel.MulePolicyModelBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.config.StartupContext;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.runtime.module.artifact.builder.TestArtifactDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultMuleDomain;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.service.ServiceManager;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.tck.util.CompilerUtils.ExtensionCompiler;
import org.mule.tck.util.CompilerUtils.JarCompiler;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;
import org.mule.test.runner.classloader.TestContainerModuleDiscoverer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.verification.VerificationMode;

@RunWith(Parameterized.class)
public class DeploymentServiceTestCase extends AbstractMuleTestCase {

  private static final int FILE_TIMESTAMP_PRECISION_MILLIS = 1000;
  protected static final int DEPLOYMENT_TIMEOUT = 10000;
  protected static final String[] NONE = new String[0];
  protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;
  private static final String MULE_POLICY_CLASSIFIER = "mule-policy";
  private static final String MULE_EXTENSION_CLASSIFIER = "mule-plugin";

  // Resources
  private static final String MULE_CONFIG_XML_FILE = getAppConfigFolderPath() + "mule-config.xml";
  private static final String MULE_DOMAIN_CONFIG_XML_FILE = Paths.get("mule", "mule-domain-config.xml").toString();
  private static final String EMPTY_APP_CONFIG_XML = "/empty-config.xml";
  private static final String BAD_APP_CONFIG_XML = "/bad-app-config.xml";
  private static final String BROKEN_CONFIG_XML = "/broken-config.xml";
  private static final String EMPTY_DOMAIN_CONFIG_XML = "/empty-domain-config.xml";
  private static final String APP_WITH_EXTENSION_PLUGIN_CONFIG = "app-with-extension-plugin-config.xml";
  private static final String APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG = "app-with-privileged-extension-plugin-config.xml";
  private static final String FOO_POLICY_NAME = "fooPolicy";
  private static final String BAR_POLICY_NAME = "barPolicy";
  private static final String BAZ_POLICY_NAME = "bazPolicy";
  private static final String FOO_POLICY_ID = "fooPolicy";
  private static final String BAR_POLICY_ID = "barPolicy";
  private static final String MIN_MULE_VERSION = "4.0.0";
  private static final String POLICY_PROPERTY_VALUE = "policyPropertyValue";
  private static final String POLICY_PROPERTY_KEY = "policyPropertyKey";
  private static final String PRIVILEGED_EXTENSION_ARTIFACT_ID = "privilegedExtensionPlugin";
  private static final String PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID = "org.mule.test:" + PRIVILEGED_EXTENSION_ARTIFACT_ID;

  private DefaultClassLoaderManager artifactClassLoaderManager;
  private ModuleRepository moduleRepository;
  private TestContainerModuleDiscoverer moduleDiscoverer;

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false},
        {true}
    });
  }

  // Dynamically compiled classes and jars
  private static File barUtils1_0JarFile;

  private static File barUtils2_0JarFile;

  private static File echoTestJarFile;

  private static File defaulServiceEchoJarFile;

  private static File defaultFooServiceJarFile;

  private static File helloExtensionV1JarFile;

  private static File helloExtensionV2JarFile;

  private static File simpleExtensionJarFile;

  private static File privilegedExtensionV1JarFile;

  private static File echoTestClassFile;

  private static File pluginEcho1TestClassFile;

  private static File pluginEcho2TestClassFile;

  private static File pluginEcho3TestClassFile;

  private static File resourceConsumerClassFile;

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    barUtils1_0JarFile =
        new JarFileBuilder("barUtils1",
                           new JarCompiler().compiling(getResourceFile("/org/bar1/BarUtils.java")).compile("bar-1.0.jar"))
                               .getArtifactFile();

    barUtils2_0JarFile = new JarCompiler().compiling(getResourceFile("/org/bar2/BarUtils.java")).compile("bar-2.0.jar");

    echoTestJarFile = new JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java")).compile("echo.jar");

    defaulServiceEchoJarFile = new JarCompiler()
        .compiling(getResourceFile("/org/mule/echo/DefaultEchoService.java"),
                   getResourceFile("/org/mule/echo/EchoServiceProvider.java"))
        .compile("mule-module-service-echo-default-4.0-SNAPSHOT.jar");

    defaultFooServiceJarFile = new JarCompiler().compiling(getResourceFile("/org/mule/service/foo/DefaultFooService.java"),
                                                           getResourceFile("/org/mule/service/foo/FooServiceProvider.java"))
        .dependingOn(defaulServiceEchoJarFile.getAbsoluteFile())
        .compile("mule-module-service-foo-default-4.0-SNAPSHOT.jar");

    helloExtensionV1JarFile = new ExtensionCompiler().compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                                                                getResourceFile("/org/foo/hello/HelloOperation.java"))
        .compile("mule-module-hello-1.0.jar", "1.0");

    helloExtensionV2JarFile = new ExtensionCompiler().compiling(getResourceFile("/org/foo/hello/HelloExtension.java"),
                                                                getResourceFile("/org/foo/hello/HelloOperation.java"))
        .compile("mule-module-hello-2.0.jar", "2.0");

    simpleExtensionJarFile = new ExtensionCompiler().compiling(getResourceFile("/org/foo/simple/SimpleExtension.java"),
                                                               getResourceFile("/org/foo/simple/SimpleOperation.java"))
        .compile("mule-module-simple-4.0-SNAPSHOT.jar", "1.0");

    privilegedExtensionV1JarFile =
        new ExtensionCompiler().compiling(getResourceFile("/org/foo/privileged/PrivilegedExtension.java"),
                                          getResourceFile("/org/foo/privileged/PrivilegedOperation.java"))
            .compile("mule-module-privileged-1.0.jar", "1.0");

    echoTestClassFile = new SingleClassCompiler().compile(getResourceFile("/org/foo/EchoTest.java"));

    pluginEcho1TestClassFile =
        new SingleClassCompiler().dependingOn(barUtils1_0JarFile).compile(getResourceFile("/org/foo/Plugin1Echo.java"));

    pluginEcho2TestClassFile =
        new SingleClassCompiler().dependingOn(barUtils2_0JarFile).compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));

    pluginEcho3TestClassFile = new SingleClassCompiler().compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));

    resourceConsumerClassFile = new SingleClassCompiler().compile(getResourceFile("/org/foo/resource/ResourceConsumer.java"));
  }

  private static File getResourceFile(String resource) throws URISyntaxException {
    return new File(DeploymentServiceTestCase.class.getResource(resource).toURI());
  }

  // Application plugin file builders
  private final ArtifactPluginFileBuilder echoPlugin = new ArtifactPluginFileBuilder("echoPlugin")
      .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
      .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile));
  private final ArtifactPluginFileBuilder echoPluginWithLib1 =
      new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
          .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
          .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  private final ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
      .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
      .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  private final ArtifactPluginFileBuilder echoPluginWithLib2 =
      new ArtifactPluginFileBuilder("echoPlugin2").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
          .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
          .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");
  private final ArtifactPluginFileBuilder pluginWithResource =
      new ArtifactPluginFileBuilder("resourcePlugin").configuredWith(EXPORTED_RESOURCE_PROPERTY, "/pluginResource.properties")
          .containingResource("pluginResourceSource.properties", "pluginResource.properties");

  private final ArtifactPluginFileBuilder pluginUsingAppResource =
      new ArtifactPluginFileBuilder("appResourcePlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.resource")
          .containingClass(resourceConsumerClassFile, "org/foo/resource/ResourceConsumer.class");

  private final ArtifactPluginFileBuilder helloExtensionV1Plugin = createHelloExtensionV1PluginFileBuilder();

  private final ArtifactPluginFileBuilder helloExtensionV2Plugin = createHelloExtensionV2PluginFileBuilder();

  private final ArtifactPluginFileBuilder simpleExtensionPlugin =
      new ArtifactPluginFileBuilder("simpleExtensionPlugin")
          .dependingOn(new JarFileBuilder("simpleExtension", simpleExtensionJarFile))
          .configuredWith(EXPORTED_RESOURCE_PROPERTY,
                          "/,  META-INF/mule-simple.xsd, META-INF/spring.handlers, META-INF/spring.schemas");

  private final ArtifactPluginFileBuilder privilegedExtensionPlugin =
      new ArtifactPluginFileBuilder(PRIVILEGED_EXTENSION_ARTIFACT_ID)
          .dependingOn(new JarFileBuilder("privilegedExtensionV1", privilegedExtensionV1JarFile))
          .configuredWith(EXPORTED_RESOURCE_PROPERTY,
                          "/,  META-INF/mule-privileged.xsd, META-INF/spring.handlers, META-INF/spring.schemas");

  // Application file builders
  private final ApplicationFileBuilder emptyAppFileBuilder =
      new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
  private final ApplicationFileBuilder globalPropertyAppFileBuilder =
      new ApplicationFileBuilder("property-app").definedBy("app-properties-config.xml");
  private final ApplicationFileBuilder dummyAppDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app")
      .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
      .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  private final ApplicationFileBuilder dummyAppDescriptorFileBuilderWithUpperCaseInExtension =
      new ApplicationFileBuilder("dummy-app", true)
          .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
          .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  private final ApplicationFileBuilder waitAppFileBuilder =
      new ApplicationFileBuilder("wait-app").definedBy("wait-app-config.xml");
  private final ApplicationFileBuilder brokenAppFileBuilder = new ApplicationFileBuilder("broken-app").corrupted();
  private final ApplicationFileBuilder incompleteAppFileBuilder =
      new ApplicationFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
  private final ApplicationFileBuilder echoPluginAppFileBuilder =
      new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml").dependingOn(echoPlugin);
  private final ApplicationFileBuilder differentLibPluginAppFileBuilder =
      new ApplicationFileBuilder("appWithLibDifferentThanPlugin").definedBy("app-plugin-different-lib-config.xml")
          .dependingOn(echoPluginWithLib1).dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
          .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");
  private final ApplicationFileBuilder multiLibPluginAppFileBuilder = new ApplicationFileBuilder("multiPluginLibVersion")
      .definedBy("multi-plugin-app-config.xml").dependingOn(echoPluginWithLib1).dependingOn(echoPluginWithLib2);
  private final ApplicationFileBuilder resourcePluginAppFileBuilder = new ApplicationFileBuilder("dummyWithPluginResource")
      .definedBy("plugin-resource-app-config.xml").dependingOn(pluginWithResource);
  private final ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
      .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
      .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));
  private final ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder =
      new ApplicationFileBuilder("broken-app+", brokenAppFileBuilder);
  private final ApplicationFileBuilder dummyDomainApp1FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app1").definedBy("empty-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder dummyDomainApp2FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app2").definedBy("empty-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder dummyDomainApp3FileBuilder = new ApplicationFileBuilder("dummy-domain-app3")
      .definedBy("bad-app-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder sharedAAppFileBuilder = new ApplicationFileBuilder("shared-app-a")
      .definedBy("shared-a-app-config.xml").deployedWith(PROPERTY_DOMAIN, "shared-domain");
  private final ApplicationFileBuilder sharedBAppFileBuilder = new ApplicationFileBuilder("shared-app-b")
      .definedBy("shared-b-app-config.xml").deployedWith(PROPERTY_DOMAIN, "shared-domain");
  private final ApplicationFileBuilder badConfigAppFileBuilder =
      new ApplicationFileBuilder("bad-config-app").definedBy("bad-app-config.xml");


  // Domain file builders
  private final DomainFileBuilder brokenDomainFileBuilder = new DomainFileBuilder("brokenDomain").corrupted();
  private final DomainFileBuilder emptyDomainFileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml");
  private final DomainFileBuilder waitDomainFileBuilder =
      new DomainFileBuilder("wait-domain").definedBy("wait-domain-config.xml");
  private final DomainFileBuilder incompleteDomainFileBuilder =
      new DomainFileBuilder("incompleteDomain").definedBy("incomplete-domain-config.xml");
  private final DomainFileBuilder invalidDomainBundleFileBuilder =
      new DomainFileBuilder("invalid-domain-bundle").definedBy("incomplete-domain-config.xml").containing(emptyAppFileBuilder);
  private final DomainFileBuilder dummyDomainBundleFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
      .definedBy("empty-domain-config.xml")
      .containing(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder).deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle"));
  private final DomainFileBuilder dummyDomainFileBuilder =
      new DomainFileBuilder("dummy-domain").definedBy("empty-domain-config.xml");
  private final DomainFileBuilder dummyUndeployableDomainFileBuilder = new DomainFileBuilder("dummy-undeployable-domain")
      .definedBy("empty-domain-config.xml").deployedWith("redeployment.enabled", "false");
  private final DomainFileBuilder sharedDomainFileBuilder =
      new DomainFileBuilder("shared-domain").definedBy("shared-domain-config.xml");
  private final DomainFileBuilder sharedBundleDomainFileBuilder = new DomainFileBuilder("shared-domain")
      .definedBy("shared-domain-config.xml").containing(sharedAAppFileBuilder).containing(sharedBAppFileBuilder);

  // Policy file builders
  private final PolicyFileBuilder fooPolicyFileBuilder =
      new PolicyFileBuilder(FOO_POLICY_NAME).describedBy(new MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION).setName(FOO_POLICY_NAME)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(FOO_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .build());

  private final PolicyFileBuilder barPolicyFileBuilder =
      new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION).setName(BAR_POLICY_NAME)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(BAR_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .build());

  private final PolicyFileBuilder policyUsingAppPluginFileBuilder =
      new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION).setName(BAR_POLICY_NAME)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(BAR_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .build());

  private final PolicyFileBuilder policyIncludingPluginFileBuilder =
      createPolicyIncludingPluginFileBuilder();

  private final PolicyFileBuilder policyIncludingHelloPluginV2FileBuilder =
      createPolicyIncludingHelloPluginV2FileBuilder();

  private final boolean parallelDeployment;
  protected File muleHome;
  protected File appsDir;
  protected File domainsDir;
  protected ServiceManager serviceManager;
  protected ExtensionModelLoaderManager extensionModelLoaderManager;
  protected MuleDeploymentService deploymentService;
  protected DeploymentListener applicationDeploymentListener;
  protected DeploymentListener domainDeploymentListener;
  private ArtifactClassLoader containerClassLoader;
  private TestPolicyManager policyManager;

  @Rule
  public SystemProperty changeChangeInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "10");

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Rule
  public TemporaryFolder compilerWorkFolder = new TemporaryFolder();

  private File services;

  public DeploymentServiceTestCase(boolean parallelDeployment) {
    this.parallelDeployment = parallelDeployment;
  }

  @Before
  public void setUp() throws Exception {

    if (parallelDeployment) {
      setProperty(PARALLEL_DEPLOYMENT_PROPERTY, "");
    }

    final String tmpDir = getProperty("java.io.tmpdir");
    muleHome = new File(new File(tmpDir, "mule_home"), getClass().getSimpleName() + currentTimeMillis());
    appsDir = new File(muleHome, "apps");
    appsDir.mkdirs();
    domainsDir = new File(muleHome, "domains");
    domainsDir.mkdirs();
    setProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

    final File domainFolder = getDomainFolder(DEFAULT_DOMAIN_NAME);
    assertThat(domainFolder.mkdirs(), is(true));

    services = getServicesFolder();
    services.mkdirs();
    copyFileToDirectory(buildSchedulerServiceFile(compilerWorkFolder.newFolder("schedulerService")), services);
    copyFileToDirectory(buildExpressionLanguageServiceFile(compilerWorkFolder.newFolder("expressionLanguageService")), services);

    applicationDeploymentListener = mock(DeploymentListener.class);
    domainDeploymentListener = mock(DeploymentListener.class);
    Set<String> privilegedArtifactIds = new HashSet<>();
    privilegedArtifactIds.add(PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID);
    moduleDiscoverer = new TestContainerModuleDiscoverer(privilegedArtifactIds);
    moduleRepository = new DefaultModuleRepository(moduleDiscoverer);
    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry =
        new MuleArtifactResourcesRegistry.Builder().moduleRepository(moduleRepository).build();
    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    containerClassLoader = muleArtifactResourcesRegistry.getContainerClassLoader();
    extensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);
    artifactClassLoaderManager = muleArtifactResourcesRegistry.getArtifactClassLoaderManager();

    deploymentService = new MuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(),
                                                  muleArtifactResourcesRegistry.getApplicationFactory(),
                                                  () -> findSchedulerService(serviceManager));
    deploymentService.addDeploymentListener(applicationDeploymentListener);
    deploymentService.addDomainDeploymentListener(domainDeploymentListener);

    policyManager = new TestPolicyManager(deploymentService,
                                          new PolicyTemplateDescriptorFactory(
                                                                              muleArtifactResourcesRegistry
                                                                                  .getArtifactPluginDescriptorLoader(),
                                                                              createDescriptorLoaderRepository()));
    // Reset test component state
    invocationCount = 0;
    policyParametrization = "";
  }

  @After
  public void tearDown() throws Exception {
    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (serviceManager != null) {
      serviceManager.stop();
    }

    if (extensionModelLoaderManager != null) {
      extensionModelLoaderManager.stop();
    }

    FileUtils.deleteTree(muleHome);

    // this is a complex classloader setup and we can't reproduce standalone Mule 100%,
    // so trick the next test method into thinking it's the first run, otherwise
    // app resets CCL ref to null and breaks the next test
    Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

    if (parallelDeployment) {
      System.clearProperty(PARALLEL_DEPLOYMENT_PROPERTY);
    }
  }

  @Test
  public void deploysAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
    final MuleRegistry registry = getMuleRegistry(app);

    // Checks that the configuration's ID was properly configured
    assertThat(app.getMuleContext().getConfiguration().getId(), equalTo(dummyAppDescriptorFileBuilder.getId()));
  }

  @Test
  public void extensionManagerPresent() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    final Application app = findApp(emptyAppFileBuilder.getId(), 1);
    assertThat(app.getMuleContext().getExtensionManager(), is(notNullValue()));
  }

  @Test
  public void appHomePropertyIsPresent() throws Exception {
    addExplodedAppFromBuilder(globalPropertyAppFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, globalPropertyAppFileBuilder.getId());

    final Application app = findApp(globalPropertyAppFileBuilder.getId(), 1);
    final MuleRegistry registry = getMuleRegistry(app);

    ConfigurationProperties configurationProperties = registry.lookupObject(ConfigurationProperties.class);
    assertThat(configurationProperties, is(notNullValue()));

    String appHome = configurationProperties.resolveStringProperty("appHome")
        .orElseThrow(() -> new RuntimeException("Could not find property appHome"));
    assertThat(new File(appHome).exists(), is(true));
  }

  @Test
  public void deploysExplodedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployExplodedWaitAppAction = () -> addExplodedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitAppAction);
  }

  @Test
  public void deploysPackagedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployPackagedWaitAppAction = () -> addPackedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitAppAction);
  }

  @Test
  public void deploysAppZipAfterStartup() throws Exception {
    deployAfterStartUp(dummyAppDescriptorFileBuilder);
  }

  @Test
  public void deploysAppZipWithExtensionUpperCaseAfterStartup() throws Exception {
    deployAfterStartUp(dummyAppDescriptorFileBuilderWithUpperCaseInExtension);
  }

  @Test
  public void deploysBrokenAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  /**
   * This tests deploys a broken app which name has a weird character. It verifies that after failing deploying that app, it
   * doesn't try to do it again, which is a behavior than can be seen in some file systems due to path handling issues
   */
  @Test
  public void doesNotRetriesBrokenAppWithFunkyName() throws Exception {
    addPackedAppFromBuilder(brokenAppWithFunkyNameAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId());
    assertAppsDir(new String[] {brokenAppWithFunkyNameAppFileBuilder.getDeployedPath()}, NONE, true);
    assertApplicationAnchorFileDoesNotExists(brokenAppWithFunkyNameAppFileBuilder.getId());

    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppWithFunkyNameAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());
  }

  @Test
  public void deploysBrokenAppZipAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(brokenAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, "broken-app");

    assertAppsDir(new String[] {"broken-app.jar"}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", "broken-app.jar", new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void redeploysAppZipDeployedOnStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
  }

  @Test
  public void redeploysAppZipDeployedAfterStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
  }

  @Test
  public void deploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Checks that dummy app was deployed just once
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void deploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysInvalidExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    String appName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    String appName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedOnlyOnce() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces", times(1));

    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    addExplodedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // After three update cycles should have only one deployment failure notification for the broken app
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
  }

  @Test
  public void deploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    String appId = incompleteAppFileBuilder.getId();
    assertZombieApplication(appId);
  }

  @Test
  public void deploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat("Configuration file does not exists", configFile.exists(), is(true));
    assertThat("Could not update last updated time in configuration file",
               configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertZombieApplication(incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    final ReentrantLock lock = deploymentService.getLock();
    lock.lock();
    try {
      File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
      assertThat(configFile.exists(), is(true));
      configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);
    } finally {
      lock.unlock();
    }

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertZombieApplication(incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(configFile.exists(), is(true));
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(EMPTY_APP_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    ReentrantLock deploymentLock = deploymentService.getLock();
    deploymentLock.lock();
    try {
      File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
      URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
      File newConfigFile = new File(url.toURI());
      copyFile(newConfigFile, originalConfigFile);
    } finally {
      deploymentLock.unlock();
    }

    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
  }

  @Test
  @Ignore("MULE-12255 Add the test plugin as a plugin of the domain")
  public void redeployModifiedDomainAndRedeployFailedApps() throws Exception {
    addExplodedDomainFromBuilder(sharedBundleDomainFileBuilder);

    // change shared config name to use a wrong name
    File domainConfigFile =
        new File(domainsDir + "/" + sharedBundleDomainFileBuilder.getDeployedPath(),
                 Paths.get("mule", DEFAULT_CONFIGURATION_RESOURCE).toString());
    String correctDomainConfigContent = IOUtils.toString(new FileInputStream(domainConfigFile));
    String wrongDomainFileContext = correctDomainConfigContent.replace("test-shared-config", "test-shared-config-wrong");
    copyInputStreamToFile(new ByteArrayInputStream(wrongDomainFileContext.getBytes()), domainConfigFile);
    long firstFileTimestamp = domainConfigFile.lastModified();

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    reset(applicationDeploymentListener);
    reset(domainDeploymentListener);

    copyInputStreamToFile(new ByteArrayInputStream(correctDomainConfigContent.getBytes()), domainConfigFile);
    alterTimestampIfNeeded(domainConfigFile, firstFileTimestamp);

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
  }

  @Test
  public void redeploysZipAppOnConfigChanges() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
  }

  @Test
  public void removesZombieFilesAfterremovesZombieFilesAfterFailedAppIsDeleted() throws Exception {
    final String appName = "bad-config-app";

    addPackedAppFromBuilder(badConfigAppFileBuilder);
    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, appName);
    assertAppsDir(new String[] {}, new String[] {appName}, true);

    final Map<URI, Long> startZombieMap = deploymentService.getZombieApplications();
    assertEquals("Should be a zombie file for the app's broken XML config", 1, startZombieMap.size());

    final Application app = findApp(badConfigAppFileBuilder.getId(), 1);
    assertStatus(app, ApplicationStatus.DEPLOYMENT_FAILED);
    assertApplicationAnchorFileDoesNotExists(app.getArtifactName());

    reset(applicationDeploymentListener);
    deleteDirectory(new File(appsDir, app.getArtifactName()));
    assertAppFolderIsDeleted(appName);
    assertAtLeastOneUndeploymentSuccess(applicationDeploymentListener, appName);

    final Map<URI, Long> endZombieMap = deploymentService.getZombieApplications();
    assertEquals("Should not be any more zombie files present", 0, endZombieMap.size());
  }

  @Test
  public void brokenAppArchiveWithoutArgument() throws Exception {
    doBrokenAppArchiveTest();
  }

  @Test
  public void brokenAppArchiveAsArgument() throws Exception {
    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", brokenAppFileBuilder.getId());
    StartupContext.get().setStartupOptions(startupOptions);

    doBrokenAppArchiveTest();
  }

  @Test
  public void deploysInvalidZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    String appName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "app with spaces.jar", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    String appName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "app with spaces.jar", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deployAppNameWithZipSuffix() throws Exception {
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("empty-app.jar", emptyAppFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    reset(applicationDeploymentListener);

    assertAppsDir(NONE, new String[] {applicationFileBuilder.getDeployedPath()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    // Checks that the empty-app.zip folder is not processed as a zip file
    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    addPackedAppFromBuilder(emptyAppFileBuilder, "1.jar");
    addPackedAppFromBuilder(emptyAppFileBuilder, "2.jar");
    addPackedAppFromBuilder(emptyAppFileBuilder, "3.jar");

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "3:1:2");
    StartupContext.get().setStartupOptions(startupOptions);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, "1");
    assertApplicationDeploymentSuccess(applicationDeploymentListener, "2");
    assertApplicationDeploymentSuccess(applicationDeploymentListener, "3");
    assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

    // When apps are passed as -app app1:app2:app3 the startup order matters
    List<Application> applications = deploymentService.getApplications();
    assertNotNull(applications);
    assertEquals(3, applications.size());
    assertEquals("3", applications.get(0).getArtifactName());
    assertEquals("1", applications.get(1).getArtifactName());
    assertEquals("2", applications.get(2).getArtifactName());
  }

  @Test
  public void deploysExplodedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    addExplodedAppFromBuilder(emptyAppFileBuilder, "1");
    addExplodedAppFromBuilder(emptyAppFileBuilder, "2");
    addExplodedAppFromBuilder(emptyAppFileBuilder, "3");

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "3:1:2");
    StartupContext.get().setStartupOptions(startupOptions);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, "1");
    assertApplicationDeploymentSuccess(applicationDeploymentListener, "2");
    assertApplicationDeploymentSuccess(applicationDeploymentListener, "3");

    assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

    // When apps are passed as -app app1:app2:app3 the startup order matters
    List<Application> applications = deploymentService.getApplications();
    assertNotNull(applications);
    assertEquals(3, applications.size());
    assertEquals("3", applications.get(0).getArtifactName());
    assertEquals("1", applications.get(1).getArtifactName());
    assertEquals("2", applications.get(2).getArtifactName());
  }

  @Test
  public void deploysAppJustOnce() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "empty-app:empty-app:empty-app");
    StartupContext.get().setStartupOptions(startupOptions);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    List<Application> applications = deploymentService.getApplications();
    assertEquals(1, applications.size());
  }

  @Test
  public void tracksAppConfigUpdateTime() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    // Sets a modification time in the future
    File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());
    File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
    configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void redeployedFailedAppAfterTouched() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());

    File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
    writeStringToFile(configFile, "you shall not pass");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
    copyFile(new File(url.toURI()), configFile);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void receivesMuleContextDeploymentNotifications() throws Exception {
    // NOTE: need an integration test like this because DefaultMuleApplication
    // class cannot be unit tested.
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertMuleContextCreated(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertMuleContextInitialized(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertMuleContextConfigured(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void undeploysStoppedApp() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    final Application app = findApp(emptyAppFileBuilder.getId(), 1);
    app.stop();
    assertStatus(app, STOPPED);

    deploymentService.undeploy(app);
  }

  @Test
  public void undeploysApplicationRemovingAnchorFile() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(app, DESTROYED);
  }

  @Test
  public void undeploysAppCompletelyEvenOnStoppingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnStopApplication(true);

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
    assertStatus(app, DESTROYED);
  }

  @Test
  public void undeploysAppCompletelyEvenOnDisposingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnDisposeApplication(true);
    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(app, STOPPED);
    assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysIncompleteZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void deploysIncompleteZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
  }

  @Test
  public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());

    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertZombieApplication(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertZombieApplication(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    assertZombieApplication(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppAfterDeploymentError() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Redeploys a fixed version for incompleteApp
    addExplodedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
  }

  @Test
  public void deploysAppZipWithPlugin() throws Exception {
    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppWithPluginSharedLibrary() throws Exception {
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

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
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
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
        .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppZipWithExtensionPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysAppZipWithPrivilegedExtensionPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("privilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(privilegedExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeploysAppZipWithInvalidPrivilegedExtensionPlugin() throws Exception {
    ArtifactPluginFileBuilder invalidPrivilegedPlugin =
        new ArtifactPluginFileBuilder("invalidPrivilegedPlugin")
            .dependingOn(new JarFileBuilder("privilegedExtensionV1", privilegedExtensionV1JarFile))
            .configuredWith(EXPORTED_RESOURCE_PROPERTY,
                            "/,  META-INF/mule-privileged.xsd, META-INF/spring.handlers, META-INF/spring.schemas");

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("invalidPrivilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(invalidPrivilegedPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPlugin() throws Exception {
    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = getByeXmlPlugin();

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPlugin")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  private ArtifactPluginFileBuilder getByeXmlPlugin() {
    final String prefixModuleName = "module-bye";
    String extensionName = "bye-extension";
    final String resources = "org/mule/module/";
    String moduleDestination = resources + prefixModuleName + ".xml";
    MulePluginModelBuilder builder = new MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriber().setId(MAVEN);
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER, MAVEN));

    return new ArtifactPluginFileBuilder(extensionName)
        .containingResource("module-byeSource.xml", moduleDestination)
        .containingResource("module-using-bye-catalogSource.xml", resources + prefixModuleName + "-catalog.xml")
        .containingResource("module-bye-type-schemaSource.json", resources + "type1-schema.json")
        .containingResource("module-bye-type-schemaSource.json", resources + "inner/folder/type2-schema.json")
        .containingResource("module-bye-type-schemaSource.json", "org/mule/type3-schema.json")
        .describedBy(builder.build());
  }

  @Test
  public void deploysWithExtensionXmlPluginWithXmlDependencies() throws Exception {
    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = getByeXmlPlugin();
    String moduleFileName = "module-using-bye.xml";
    String extensionName = "using-bye-extension";
    String moduleDestination = "org/mule/module/" + moduleFileName;
    MulePluginModelBuilder builder =
        new MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriber().addProperty(EXPORTED_PACKAGES, asList("org.foo")).setId(MAVEN);
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER, MAVEN));

    final ArtifactPluginFileBuilder usingByeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .containingResource("module-using-byeSource.xml", moduleDestination)
        .dependingOn(byeXmlExtensionPlugin)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginWithXmlDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-bye.xml")
        .dependingOn(usingByeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  private ServiceRegistryDescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());
  }

  @Test
  public void deploysWithExtensionXmlPluginWithDependencies() throws Exception {
    String moduleFileName = "module-using-java.xml";
    String extensionName = "using-java-extension";
    String moduleDestination = "org/mule/module/" + moduleFileName;
    MulePluginModelBuilder builder =
        new MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriber().addProperty(EXPORTED_PACKAGES, asList("org.foo")).setId(MAVEN);
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER, MAVEN));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build())
        .containingResource("module-using-javaSource.xml", moduleDestination)
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile))
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginWithDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-java.xml")
        .dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeployWithExtensionThatHasNonExistingIdForExtensionModel() throws Exception {
    String extensionName = "extension-with-extension-model-id-non-existing";
    MulePluginModelBuilder builder =
        new MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId("a-non-existing-ID-describer").addProperty("aProperty", "aValue");
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER,
                                                                    PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeployWithExtensionThatHasNonExistingIdForClassLoaderModel() throws Exception {
    String extensionName = "extension-with-classloader-model-id-non-existing";

    MulePluginModelBuilder builder =
        new MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withClassLoaderModelDescriber().setId("a-non-existing-ID-describer").addProperty("aProperty", "aValue");
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER,
                                                                    PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    // TODO(fernandezlautaro): MULE-11089 the following line is expecting to see one deploy, for some reason tries to redeploy two
    // times so I added until this bug gets fixed
    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId(), times(2));
  }

  @Test
  public void deploysAppWithPluginBootstrapProperty() throws Exception {
    final ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, BOOTSTRAP_PROPERTIES);

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("app-with-plugin-bootstrap")
        .definedBy("app-with-plugin-bootstrap.xml").dependingOn(pluginFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    final Application application = findApp(applicationFileBuilder.getId(), 1);
    final Object lookupObject = application.getMuleContext().getRegistry().lookupObject("plugin.echotest");
    assertThat(lookupObject, is(not(nullValue())));
    assertThat(lookupObject.getClass().getName(), equalTo("org.foo.EchoTest"));
  }

  @Test
  public void failsToDeployApplicationOnMissingService() throws Exception {
    ArtifactPluginFileBuilder extensionPlugin = new ArtifactPluginFileBuilder("extensionPlugin")
        .dependingOn(new JarFileBuilder("bundleExtensionv1", helloExtensionV1JarFile))
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, "/, META-INF");
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG).dependingOn(extensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void synchronizesAppDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onDeploymentStart(dummyAppDescriptorFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesAppUndeployFromClient() throws Exception {
    final Action action = () -> deploymentService.undeploy(emptyAppFileBuilder.getId());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onUndeploymentStart(emptyAppFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesAppRedeployFromClient() throws Exception {
    final Action action = () -> {
      // Clears notification from first deployment
      reset(applicationDeploymentListener);
      deploymentService.redeploy(emptyAppFileBuilder.getId());
    };

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onDeploymentStart(emptyAppFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  private void doSynchronizedAppDeploymentActionTest(final Action deploymentAction, final Action assertAction) throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, applicationDeploymentListener,
                                               emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationSharedLib() throws Exception {
    final String domainId = "shared-lib";
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("shared-lib-precedence-app")
        .definedBy("app-shared-lib-precedence-config.xml")
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
        .deployedWith(PROPERTY_DOMAIN, domainId);
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml")
            .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationLib() throws Exception {
    final String domainId = "shared-lib";
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOnSharedLibrary(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
            .deployedWith(PROPERTY_DOMAIN, domainId);
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1_0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml")
            .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationPluginLib() throws Exception {
    final String domainId = "shared-lib";
    final ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
            .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOn(pluginFileBuilder).deployedWith(PROPERTY_DOMAIN, domainId);

    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1.0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml")
            .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysMultiPluginVersionLib() throws Exception {
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

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysLightApplicationWithPluginDependingOnPlugin() throws Exception {

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin).usingLightwayPackage();
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

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
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

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (MessagingException e) {
      assertThat(e.getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getMessage(), containsString("org/foo/EchoTest"));
    }
  }

  @Test
  public void failsToDeployApplicationWithPluginDependantOnPluginNotShipped() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, artifactFileBuilder.getId(), times(1));
  }

  @Test
  public void deploysAppWithLibDifferentThanPlugin() throws Exception {
    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingPluginResource() throws Exception {
    addPackedAppFromBuilder(resourcePluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, resourcePluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppProvidingResourceForPlugin() throws Exception {
    final TestArtifactDescriptor artifactFileBuilder =
        new ApplicationFileBuilder("appProvidingResourceForPlugin")
            .definedBy("app-providing-resource-for-plugin.xml")
            .dependingOn(pluginUsingAppResource)
            .configuredWith(EXPORTED_RESOURCES, "META-INF/app-resource.txt")
            .usingResource(getResourceFile("/test-resource.txt").toString(), "META-INF/app-resource.txt");
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void synchronizesDeploymentOnStart() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    Thread deploymentServiceThread = new Thread(() -> {
      try {
        startDeployment();
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    });

    final boolean[] lockedFromClient = new boolean[1];

    doAnswer(invocation -> {

      Thread deploymentClientThread = new Thread(() -> {
        ReentrantLock deploymentLock = deploymentService.getLock();

        try {
          try {
            lockedFromClient[0] = deploymentLock.tryLock(1000, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            // Ignore
          }
        } finally {
          if (deploymentLock.isHeldByCurrentThread()) {
            deploymentLock.unlock();
          }
        }
      });

      deploymentClientThread.start();
      deploymentClientThread.join();

      return null;
    }).when(applicationDeploymentListener).onDeploymentStart(emptyAppFileBuilder.getId());

    deploymentServiceThread.start();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
  }

  @Test
  public void deploysMultipleAppsZipOnStartup() throws Exception {
    final int totalApps = 20;

    for (int i = 1; i <= totalApps; i++) {
      addExplodedAppFromBuilder(emptyAppFileBuilder, Integer.toString(i));
    }

    startDeployment();

    for (int i = 1; i <= totalApps; i++) {
      assertDeploymentSuccess(applicationDeploymentListener, Integer.toString(i));
    }
  }

  @Test
  public void deploysDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getMuleContext());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysPackagedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployPackagedWaitDomainAction = () -> addPackedDomainFromBuilder(waitDomainFileBuilder);
    deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitDomainAction);
  }

  @Test
  public void deploysExplodedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployExplodedWaitDomainAction = () -> addExplodedDomainFromBuilder(waitDomainFileBuilder);
    deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitDomainAction);
  }

  @Test
  public void deploysExplodedDomainBundleOnStartup() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

    startDeployment();

    deploysDomainBundle();
  }

  @Test
  public void deploysExplodedDomainBundleAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

    deploysDomainBundle();
  }

  @Test
  public void deploysDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

    startDeployment();

    deploysDomainBundle();
  }

  @Test
  public void deploysDomainBundleZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

    deploysDomainBundle();
  }

  private void deploysDomainBundle() {
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainBundleFileBuilder.getId()}, true);

    final Domain domain = findADomain(dummyDomainBundleFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getMuleContext());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
    assertNotNull(app);
  }

  @Test
  public void deploysInvalidExplodedDomainBundleOnStartup() throws Exception {
    addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidDomainBundleZipAfterStartup() throws Exception {
    addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  private void deploysInvalidDomainBundleZip() {
    assertDeploymentFailure(domainDeploymentListener, invalidDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, invalidDomainBundleFileBuilder.getId()}, true);

    assertAppsDir(NONE, new String[] {}, true);
  }

  @Test
  public void deploysDomainZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getMuleContext());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysBrokenDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as domain", brokenDomainFileBuilder.getDeployedPath(),
                 new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysBrokenDomainZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as domain.", brokenDomainFileBuilder.getDeployedPath(),
                 new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void redeploysDomainZipDeployedOnStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, emptyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
  }

  @Test
  public void redeployedDomainsAreDifferent() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, emptyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    Domain firstDomain = findADomain(emptyDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    Domain secondDomain = findADomain(emptyDomainFileBuilder.getId());

    assertNotSame(firstDomain, secondDomain);
  }

  @Test
  public void redeploysDomainZipRefreshesApps() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
  }

  @Test
  public void redeploysDomainZipDeployedAfterStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
  }

  @Test
  public void deploysAppUsingDomainPlugin() throws Exception {
    ApplicationFileBuilder echoPluginAppFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml")
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle");

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(echoPlugin)
        .containing(echoPluginAppFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingDomainExtension() throws Exception {
    installEchoService();
    installFooService();

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("appWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle");

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin)
        .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void failsToDeployAppWithDomainPluginVersionMismatch() throws Exception {
    installEchoService();
    installFooService();

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle")
            .dependingOn(helloExtensionV2Plugin);

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin)
        .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void appliesApplicationPolicyUsingDomainPlugin() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle");

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin)
        .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  public void appliesApplicationPolicyDuplicatingDomainPlugin() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle");

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin)
        .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  public void failsToApplyApplicationPolicyWithDomainPluginVersionMismatch() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingHelloPluginV2FileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle");

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin)
        .containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingHelloPluginV2FileBuilder.getId(),
                              new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                        getResourceFile("/appPluginPolicy.xml")));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
    }
  }

  protected void alterTimestampIfNeeded(File file, long firstTimestamp) {
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exists: " + file.getAbsolutePath());
    }
    if (firstTimestamp == file.lastModified()) {
      // File systems only have second precision. If both file writes happen during the same second, the last
      // change will be ignored by the directory scanner.
      assertThat(file.setLastModified(file.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));
    }
  }

  @Test
  public void deploysExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder);
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Checks that dummy app was deployed just once
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysInvalidExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, "domain with spaces"}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    String domainName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "domain with spaces", domainName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, "domain with spaces"}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = new File(zombie.getKey()).getName();
    assertEquals("Wrong URL tagged as zombie.", "domain with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedDomainOnlyOnce() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");
    assertDeploymentFailure(domainDeploymentListener, "domain with spaces", times(1));

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "empty2-domain");
    assertDeploymentSuccess(domainDeploymentListener, "empty2-domain");

    // After three update cycles should have only one deployment failure notification for the broken app
    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
  }

  @Test
  public void deploysBrokenExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, incompleteDomainFileBuilder.getId()}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysBrokenExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, incompleteDomainFileBuilder.getId()}, true);
    final Map<URI, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  @Ignore("MULE-12255 Add the test plugin as a plugin of the domain")
  public void receivesDomainMuleContextDeploymentNotifications() throws Exception {
    // NOTE: need an integration test like this because DefaultMuleApplication
    // class cannot be unit tested.
    addPackedDomainFromBuilder(sharedDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextCreated(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextInitialized(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextConfigured(domainDeploymentListener, sharedDomainFileBuilder.getId());
  }

  @Test
  public void undeploysStoppedDomain() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    domain.stop();

    deploymentService.undeploy(domain);
  }

  @Test
  public void undeploysDomainRemovingAnchorFile() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
  }

  @Test
  public void undeploysDomainAndDomainsApps() throws Exception {
    doDomainUndeployAndVerifyAppsAreUndeployed(() -> {
      Domain domain = findADomain(dummyDomainFileBuilder.getId());
      deploymentService.undeploy(domain);
    });
  }

  @Test
  public void undeploysDomainAndDomainsAppsRemovingAnchorFile() throws Exception {
    doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());
  }

  @Test
  public void undeployDomainDoesNotDeployAllApplications() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());

    assertThat(findApp(emptyAppFileBuilder.getId(), 1), notNullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void findDomainApplicationsWillNullDomainFails() {
    deploymentService.findDomainApplications(null);
  }

  @Test
  public void findDomainApplicationsWillNonExistentDomainReturnsEmptyCollection() {
    Collection<Application> domainApplications = deploymentService.findDomainApplications("");
    assertThat(domainApplications, notNullValue());
    assertThat(domainApplications.isEmpty(), is(true));
  }

  @Test
  public void undeploysDomainCompletelyEvenOnStoppingException() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    TestDomainFactory testDomainFactory =
        TestDomainFactory.createDomainFactory(new DomainClassLoaderFactory(containerClassLoader.getClassLoader()),
                                              containerClassLoader, serviceManager, moduleRepository,
                                              createDescriptorLoaderRepository());
    testDomainFactory.setFailOnStopApplication();

    deploymentService.setDomainFactory(testDomainFactory);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
  }

  @Test
  public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    TestDomainFactory testDomainFactory =
        TestDomainFactory.createDomainFactory(new DomainClassLoaderFactory(containerClassLoader.getClassLoader()),
                                              containerClassLoader, serviceManager, moduleRepository,
                                              createDescriptorLoaderRepository());
    testDomainFactory.setFailOnDisposeApplication();
    deploymentService.setDomainFactory(testDomainFactory);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysIncompleteZipDomainOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void deploysIncompleteZipDomainAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Deploys another domain to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  @Test
  public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(dummyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void refreshDomainClassloaderAfterRedeployment() throws Exception {
    startDeployment();

    // Deploy domain and apps and wait until success
    addPackedDomainFromBuilder(sharedDomainFileBuilder);
    addPackedAppFromBuilder(sharedAAppFileBuilder);
    addPackedAppFromBuilder(sharedBAppFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    // Ensure resources are registered at domain's registry
    Domain domain = findADomain(sharedDomainFileBuilder.getId());
    assertThat(domain.getMuleContext().getRegistry().get("http-listener-config"), not(is(nullValue())));

    ArtifactClassLoader initialArtifactClassLoader = domain.getArtifactClassLoader();

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    // Force redeployment by touching the domain's config file
    File domainFolder = new File(domainsDir.getPath(), sharedDomainFileBuilder.getId());
    File configFile = new File(domainFolder, sharedDomainFileBuilder.getConfigFile());
    long firstFileTimestamp = configFile.lastModified();
    touch(configFile);
    alterTimestampIfNeeded(configFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    domain = findADomain(sharedDomainFileBuilder.getId());
    ArtifactClassLoader artifactClassLoaderAfterRedeployment = domain.getArtifactClassLoader();

    // Ensure that after redeployment the domain's class loader has changed
    assertThat(artifactClassLoaderAfterRedeployment, not(sameInstance(initialArtifactClassLoader)));

    // Undeploy domain and apps
    removeAppAnchorFile(sharedAAppFileBuilder.getId());
    removeAppAnchorFile(sharedBAppFileBuilder.getId());
    removeDomainAnchorFile(sharedDomainFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());

    assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());
    assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    final Map.Entry<URI, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey()).getParentFile().getParentFile().getName());
  }

  @Test
  public void redeploysExplodedDomainAfterDeploymentError() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Redeploys a fixed version for incompleteDomain
    addExplodedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());
    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  private Action createUndeployDummyDomainAction() {
    return () -> removeDomainAnchorFile(dummyDomainFileBuilder.getId());
  }

  private void doDomainUndeployAndVerifyAppsAreUndeployed(Action undeployAction) throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    addPackedAppFromBuilder(dummyDomainApp2FileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    deploymentService.getLock().lock();
    try {
      undeployAction.perform();
    } finally {
      deploymentService.getLock().unlock();
    }

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
  }

  @Test
  public void redeploysFixedDomainAfterBrokenExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    doRedeployFixedDomainAfterBrokenDomain();
  }

  @Test
  public void redeploysFixedDomainAfterBrokenExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    doRedeployFixedDomainAfterBrokenDomain();
  }

  @Test
  public void redeploysDomainAndItsApplications() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    doRedeployDummyDomainByChangingConfigFileWithGoodOne();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void doesNotRedeployDomainWithRedeploymentDisabled() throws Exception {
    addExplodedDomainFromBuilder(dummyUndeployableDomainFileBuilder, dummyUndeployableDomainFileBuilder.getId());
    addPackedAppFromBuilder(emptyAppFileBuilder, "empty-app.jar");

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyUndeployableDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    // change domain and app since once the app redeploys we can check the domain did not
    doRedeployDomainByChangingConfigFileWithGoodOne(dummyUndeployableDomainFileBuilder);
    doRedeployAppByChangingConfigFileWithGoodOne(emptyAppFileBuilder.getDeployedPath());

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    verify(domainDeploymentListener, never()).onDeploymentSuccess(dummyUndeployableDomainFileBuilder.getId());
  }

  @Test
  public void redeploysDomainAndFails() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    doRedeployDummyDomainByChangingConfigFileWithBadOne();

    assertDeploymentFailure(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void redeploysDomainWithOneApplicationFailedOnFirstDeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp3FileBuilder, dummyDomainApp3FileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    deploymentService.getLock().lock();
    try {
      doRedeployDummyDomainByChangingConfigFileWithGoodOne();
      doRedeployAppByChangingConfigFileWithGoodOne(dummyDomainApp3FileBuilder.getDeployedPath());
    } finally {
      deploymentService.getLock().unlock();
    }

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());
  }

  @Test
  public void redeploysDomainWithOneApplicationFailedAfterRedeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    deploymentService.getLock().lock();
    try {
      doRedeployDummyDomainByChangingConfigFileWithGoodOne();
      doRedeployAppByChangingConfigFileWithBadOne(dummyDomainApp2FileBuilder.getDeployedPath());
    } finally {
      deploymentService.getLock().unlock();
    }

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void deployFailsWhenMissingFile() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    forceDelete(originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(emptyAppFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void synchronizesDomainDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deployDomain(dummyDomainFileBuilder.getArtifactFile().toURI());

    final Action assertAction = () -> verify(domainDeploymentListener, never()).onDeploymentStart(dummyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesDomainUndeployFromClient() throws Exception {
    final Action action = () -> deploymentService.undeployDomain(emptyDomainFileBuilder.getId());

    final Action assertAction =
        () -> verify(domainDeploymentListener, never()).onUndeploymentStart(emptyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesDomainRedeployFromClient() throws Exception {
    final Action action = () -> {
      // Clears notification from first deployment
      reset(domainDeploymentListener);
      deploymentService.redeployDomain(emptyDomainFileBuilder.getId());
    };

    final Action assertAction = () -> verify(domainDeploymentListener, never()).onDeploymentStart(emptyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  @Test
  public void appliesApplicationPolicy() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> true, 1, POLICY_PROPERTY_VALUE);
  }

  @Test
  public void appliesMultipleApplicationPolicies() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(barPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml")));
    policyManager.addPolicy(applicationFileBuilder.getId(), barPolicyFileBuilder.getId(),
                            new PolicyParametrization(BAR_POLICY_ID, poinparameters -> true, 2, emptyMap(),
                                                      getResourceFile("/barPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(2));
  }

  @Test
  public void failsToApplyBrokenApplicationPolicy() throws Exception {
    PolicyFileBuilder brokenPolicyFileBuilder =
        new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModelBuilder()
            .setMinMuleVersion(MIN_MULE_VERSION).setName(BAR_POLICY_NAME)
            .withBundleDescriptorLoader(createBundleDescriptorLoader(BAR_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .build());

    policyManager.registerPolicyTemplate(brokenPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), brokenPolicyFileBuilder.getId(),
                              new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1, emptyMap(),
                                                        getResourceFile("/brokenPolicy.xml")));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
    }
  }

  @Test
  public void skipsApplicationPolicy() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> false, 0, "");
  }

  private void doApplicationPolicyExecutionTest(PolicyPointcut pointcut, int expectedPolicyInvocations,
                                                Object expectedPolicyParametrization)
      throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointcut, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml")));


    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(expectedPolicyInvocations));
    assertThat(policyParametrization, equalTo(expectedPolicyParametrization));
  }

  @Test
  public void removesApplicationPolicy() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));

    policyManager.removePolicy(applicationFileBuilder.getId(), FOO_POLICY_ID);

    executeApplicationFlow("main");
    assertThat("Policy is still applied on the application", invocationCount, equalTo(1));
  }


  @Test
  public void appliesApplicationPolicyUsingAppPlugin() throws Exception {
    policyManager.registerPolicyTemplate(policyUsingAppPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyUsingAppPluginFileBuilder.getId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  public void appliesApplicationPolicyIncludingPlugin() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
                                                                                           simpleExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  public void appliesApplicationPolicyDuplicatingPlugin() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml")));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  public void failsToApplyApplicationPolicyWithPluginVersionMismatch() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingHelloPluginV2FileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingHelloPluginV2FileBuilder.getId(),
                              new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                        getResourceFile("/appPluginPolicy.xml")));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
    }
  }

  private ApplicationFileBuilder createExtensionApplicationWithServices(String appConfigFile,
                                                                        ArtifactPluginFileBuilder... plugins)
      throws Exception {
    installEchoService();
    installFooService();

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy(appConfigFile);

    for (ArtifactPluginFileBuilder plugin : plugins) {
      applicationFileBuilder.dependingOn(plugin);
    }

    return applicationFileBuilder;
  }

  private void installFooService() throws IOException {
    installService("fooService", "org.mule.service.foo.FooServiceProvider", defaultFooServiceJarFile);
  }

  private void installEchoService() throws IOException {
    installService("echoService", "org.mule.echo.EchoServiceProvider", defaulServiceEchoJarFile);
  }

  private void installService(String serviceName, String serviceProviderClassName, File serviceJarFile) throws IOException {
    final ServiceFileBuilder echoService =
        new ServiceFileBuilder(serviceName).configuredWith(SERVICE_PROVIDER_CLASS_NAME, serviceProviderClassName)
            .usingLibrary(serviceJarFile.getAbsolutePath());
    File installedService = new File(services, echoService.getArtifactFile().getName());
    copyFile(echoService.getArtifactFile(), installedService);
  }

  private void doSynchronizedDomainDeploymentActionTest(final Action deploymentAction, final Action assertAction)
      throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    final DeploymentListener domainDeploymentListener = this.domainDeploymentListener;
    final String artifactId = emptyDomainFileBuilder.getId();

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, domainDeploymentListener, artifactId);
  }

  private void doSynchronizedArtifactDeploymentActionTest(final Action deploymentAction, final Action assertAction,
                                                          DeploymentListener domainDeploymentListener, String artifactId) {
    Thread deploymentServiceThread = new Thread(() -> {
      try {
        startDeployment();
      } catch (MuleException e) {
        throw new RuntimeException("Unable to start deployment service");
      }
    });

    final boolean[] deployedFromClient = new boolean[1];

    doAnswer(invocation -> {

      Thread deploymentClientThread = new Thread(() -> {
        try {
          deploymentAction.perform();
        } catch (Exception e) {
          // Ignore
        }
      });

      deploymentClientThread.start();
      deploymentClientThread.join();
      try {
        assertAction.perform();
      } catch (AssertionError e) {
        deployedFromClient[0] = true;
      }

      return null;
    }).when(domainDeploymentListener).onDeploymentStart(artifactId);

    deploymentServiceThread.start();

    assertDeploymentSuccess(domainDeploymentListener, artifactId);

    assertFalse("Able to perform a deployment action while another deployment operation was in progress", deployedFromClient[0]);
  }

  private void doRedeployAppByChangingConfigFileWithGoodOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, EMPTY_APP_CONFIG_XML);
  }

  private void doRedeployAppByChangingConfigFileWithBadOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, BAD_APP_CONFIG_XML);
  }

  private void changeConfigFile(String applicationPath, String configFile) throws Exception {
    File originalConfigFile = new File(new File(appsDir, applicationPath), MULE_CONFIG_XML_FILE);
    assertThat("Original config file doe snot exists: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  private void doRedeployDummyDomainByChangingConfigFileWithGoodOne() throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", dummyDomainFileBuilder);
  }

  private void doRedeployDomainByChangingConfigFileWithGoodOne(DomainFileBuilder domain) throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", domain);
  }

  private void doRedeployDummyDomainByChangingConfigFileWithBadOne() throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/bad-domain-config.xml", dummyDomainFileBuilder);
  }

  private void doRedeployDomainByChangingConfigFile(String configFile, DomainFileBuilder domain)
      throws URISyntaxException, IOException {
    File originalConfigFile =
        new File(new File(domainsDir, domain.getDeployedPath()), "mule" + File.separator + domain.getConfigFile());
    assertThat("Cannot find domain config file: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  private void doRedeployFixedDomainAfterBrokenDomain() throws Exception {
    assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

    reset(domainDeploymentListener);

    File originalConfigFile = new File(domainsDir + "/incompleteDomain", DEFAULT_CONFIGURATION_RESOURCE_LOCATION);
    URL url = getClass().getResource("/empty-domain-config.xml");
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained("incompleteDomain");
  }

  public void doBrokenAppArchiveTest() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    // let the file system's write-behind cache commit the delete operation?
    Thread.sleep(FILE_TIMESTAMP_PRECISION_MILLIS);

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);
    // don't assert dir contents, we want to check internal deployer state next
    assertAppsDir(NONE, new String[] {brokenAppFileBuilder.getId()}, false);
    assertEquals("No apps should have been registered with Mule.", 0, deploymentService.getApplications().size());
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

    // Checks that the invalid zip was not deployed again
    try {
      assertDeploymentFailure(applicationDeploymentListener, "broken-app.jar");
      fail("Install was invoked again for the broken application file");
    } catch (AssertionError expected) {
    }
  }

  private void deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExistsAction = () -> assertApplicationAnchorFileDoesNotExists(waitAppFileBuilder.getId());
    Action verifyDeploymentSuccessfulAction =
        () -> assertApplicationDeploymentSuccess(applicationDeploymentListener, waitAppFileBuilder.getId());
    Action verifyAnchorFileExistsAction = () -> assertApplicationAnchorFileExists(waitAppFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExistsAction,
                                                                verifyDeploymentSuccessfulAction, verifyAnchorFileExistsAction);
  }

  private void deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExists = () -> assertDomainAnchorFileDoesNotExists(waitDomainFileBuilder.getId());
    Action verifyDeploymentSuccessful = () -> assertDeploymentSuccess(domainDeploymentListener, waitDomainFileBuilder.getId());
    Action verifyAnchorFileExists = () -> assertDomainAnchorFileExists(waitDomainFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExists,
                                                                verifyDeploymentSuccessful, verifyAnchorFileExists);
  }

  private void deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(Action deployArtifactAction,
                                                                           Action verifyAnchorFileDoesNotExistsAction,
                                                                           Action verifyDeploymentSuccessfulAction,
                                                                           Action verifyAnchorFileExistsAction)
      throws Exception {
    WaitComponent.reset();
    startDeployment();
    deployArtifactAction.perform();
    try {
      if (!WaitComponent.componentInitializedLatch.await(DEPLOYMENT_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("WaitComponent should be initilaized already. Probably app deployment failed");
      }
      verifyAnchorFileDoesNotExistsAction.perform();
    } finally {
      WaitComponent.waitLatch.release();
    }
    verifyDeploymentSuccessfulAction.perform();
    verifyAnchorFileExistsAction.perform();
  }

  private void startDeployment() throws MuleException {
    serviceManager.start();
    extensionModelLoaderManager.start();
    deploymentService.start();
  }

  private void assertApplicationDeploymentSuccess(DeploymentListener listener, String artifactName) {
    assertDeploymentSuccess(listener, artifactName);
    assertStatus(artifactName, ApplicationStatus.STARTED);
  }

  private void assertDeploymentSuccess(final DeploymentListener listener, final String artifactName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(listener, times(1)).onDeploymentSuccess(artifactName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to deploy application: " + artifactName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextCreated(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextCreated(eq(appName), any(MuleContext.class), any(CustomizationService.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextCreated", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextInitialized(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextInitialised(eq(appName), any(MuleContext.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextInitialised", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextConfigured(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextConfigured(eq(appName), any(MuleContext.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextConfigured", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onUndeploymentSuccess(appName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to undeploy artifact: " + appName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertAtLeastOneUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, atLeastOnce()).onUndeploymentSuccess(appName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to undeploy artifact: " + appName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private MuleRegistry getMuleRegistry(Application app) {
    return withContextClassLoader(app.getArtifactClassLoader().getClassLoader(), () -> app.getMuleContext().getRegistry());
  }

  private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName) {
    assertDeploymentFailure(listener, artifactName, times(1));
  }

  private void assertStatus(String appName, ApplicationStatus status) {
    assertStatus(appName, status, -1);
  }

  private void assertStatus(String appName, ApplicationStatus status, int expectedApps) {
    Application app = findApp(appName, expectedApps);
    assertThat(app, notNullValue());
    assertStatus(app, status);
  }

  private void assertStatus(final Application application, final ApplicationStatus status) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(application.getStatus(), is(status));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Application %s was expected to be in status %s but was %s instead", application.getArtifactName(),
                             status.name(), application.getStatus().name());
      }
    });

  }

  private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName,
                                       final VerificationMode mode) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, mode).onDeploymentFailure(eq(artifactName), any(Throwable.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Application deployment was supposed to fail for: " + artifactName + super.describeFailure();
      }
    });
  }

  private void assertNoDeploymentInvoked(final DeploymentListener deploymentListener) {
    // TODO(pablo.kraan): look for a better way to test this
    boolean invoked;
    Prober prober = new PollingProber(DeploymentDirectoryWatcher.DEFAULT_CHANGES_CHECK_INTERVAL_MS * 2, 100);
    try {
      prober.check(new Probe() {

        @Override
        public boolean isSatisfied() {
          try {
            verify(deploymentListener, times(1)).onDeploymentStart(any(String.class));
            return true;
          } catch (AssertionError e) {
            return false;
          }
        }

        @Override
        public String describeFailure() {
          return "No deployment has started";
        }
      });

      invoked = true;
    } catch (AssertionError e) {
      invoked = false;
    }

    assertFalse("A deployment was started", invoked);
  }

  /**
   * Find a deployed app, performing some basic assertions.
   */
  private Application findApp(final String appName, int totalAppsExpected) {
    // list all apps to validate total count
    final List<Application> apps = deploymentService.getApplications();
    assertNotNull(apps);

    if (totalAppsExpected >= 0) {
      assertEquals(totalAppsExpected, apps.size());
    }

    final Application app = deploymentService.findApplication(appName);
    assertNotNull(app);
    return app;
  }

  private DefaultMuleDomain createDefaultDomain() {
    return new DefaultMuleDomain(new DomainDescriptor(DEFAULT_DOMAIN_NAME),
                                 new DomainClassLoaderFactory(getClass().getClassLoader())
                                     .create("domain/" + DEFAULT_DOMAIN_NAME, containerClassLoader,
                                             new DomainDescriptor(DEFAULT_DOMAIN_NAME), emptyList()),
                                 artifactClassLoaderManager, serviceManager, emptyList(), extensionModelLoaderManager);
  }

  /**
   * Finds a deployed domain
   */
  private Domain findADomain(final String domainName) {
    final Domain domain = deploymentService.findDomain(domainName);
    assertNotNull(domain);
    return domain;
  }

  private void assertAppsDir(String[] expectedZips, String[] expectedApps, boolean performValidation) {
    assertArtifactDir(appsDir, expectedZips, expectedApps, performValidation);
  }

  private void assertDomainDir(String[] expectedZips, String[] expectedDomains, boolean performValidation) {
    assertArtifactDir(domainsDir, expectedZips, expectedDomains, performValidation);
  }

  private void assertArtifactDir(File artifactDir, String[] expectedZips, String[] expectedArtifacts, boolean performValidation) {
    final String[] actualZips = artifactDir.list(MuleDeploymentService.JAR_ARTIFACT_FILTER);
    if (performValidation) {
      assertArrayEquals("Invalid Mule artifact archives set", expectedZips, actualZips);
    }
    final String[] actualArtifacts = artifactDir.list(DIRECTORY);
    if (performValidation) {
      assertTrue("Invalid Mule exploded artifact set",
                 isEqualCollection(asList(expectedArtifacts), asList(actualArtifacts)));
    }
  }

  private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedAppFromBuilder(artifactFileBuilder, null);
  }

  private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addPackedAppArchive(artifactFileBuilder, targetName);
  }

  private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedDomainFromBuilder(artifactFileBuilder, null);
  }

  private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addArchive(domainsDir, artifactFileBuilder.getArtifactFile().toURI(), targetName);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   */
  private void addPackedAppArchive(TestArtifactDescriptor artifactFileBuilder, String targetFile) throws Exception {
    addArchive(appsDir, artifactFileBuilder.getArtifactFile().toURI(), targetFile);
  }

  private void addArchive(File outputDir, URI uri, String targetFile) throws Exception {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
      final String tempFileName = (targetFile == null ? new File(uri) : new File(targetFile)).getName() + ".part";
      final File tempFile = new File(outputDir, tempFileName);
      copyFile(new File(uri), tempFile);
      final File destFile = new File(removeEnd(tempFile.getAbsolutePath(), ".part"));
      File deployFolder = new File(destFile.getAbsolutePath().replace(JAR_FILE_SUFFIX, ""));
      if (deployFolder.exists()) {
        // Delete META-INF folder so maven file do not get duplicated during redeployment testing.
        deleteDirectory(new File(deployFolder, "META-INF"));
      }
      tempFile.renameTo(destFile);
      assertThat("File does not exists: " + destFile.getAbsolutePath(), destFile.exists(), is(true));
    } finally {
      lock.unlock();
    }
  }

  private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedAppFromBuilder(artifactFileBuilder, null);
  }

  private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, appName, MULE_CONFIG_XML_FILE, appsDir);
  }

  private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedDomainFromBuilder(artifactFileBuilder, null);
  }

  private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String domainName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, domainName, MULE_DOMAIN_CONFIG_XML_FILE, domainsDir);
  }

  private void addExplodedArtifactFromBuilder(TestArtifactDescriptor artifactFileBuilder, String artifactName,
                                              String configFileName, File destinationDir)
      throws Exception {
    addExplodedArtifactFromUrl(artifactFileBuilder.getArtifactFile().toURI().toURL(), artifactName, configFileName,
                               destinationDir);
  }

  private void addExplodedArtifactFromUrl(URL resource, String artifactName, String configFileName, File destinationDir)
      throws Exception, URISyntaxException {
    assertNotNull("Resource URL cannot be null", resource);

    String artifactFolder = artifactName;
    if (artifactFolder == null) {
      File file = toFile(resource);
      int index = file.getName().lastIndexOf(".");

      if (index > 0) {
        artifactFolder = file.getName().substring(0, index);
      } else {
        artifactFolder = file.getName();
      }
    }

    addExplodedArtifact(resource, artifactFolder, configFileName, destinationDir);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   */
  private void addExplodedArtifact(URL url, String artifactName, String configFileName, File destinationDir)
      throws Exception, URISyntaxException {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      File tempFolder = new File(muleHome, artifactName);
      FileUtils.unzip(new File(url.toURI()), tempFolder);

      // Under some platforms, file.lastModified is managed at second level, not milliseconds.
      // Need to update the config file lastModified ere to ensure that is different from previous value
      File configFile = new File(tempFolder, configFileName);
      if (configFile.exists()) {
        configFile.setLastModified(System.currentTimeMillis() + FILE_TIMESTAMP_PRECISION_MILLIS);
      }

      File appFolder = new File(destinationDir, artifactName);

      if (appFolder.exists()) {
        FileUtils.deleteTree(appFolder);
      }

      moveDirectory(tempFolder, appFolder);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Removes a given application anchor file in order to start application undeployment
   *
   * @param appName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  private boolean removeAppAnchorFile(String appName) {
    File anchorFile = getArtifactAnchorFile(appName, appsDir);
    return anchorFile.delete();
  }

  /**
   * Removes a given domain anchor file in order to start application undeployment
   *
   * @param domainName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  private boolean removeDomainAnchorFile(String domainName) {
    File anchorFile = getArtifactAnchorFile(domainName, domainsDir);
    return anchorFile.delete();
  }

  private void assertApplicationAnchorFileExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(true));
  }

  private void assertApplicationAnchorFileDoesNotExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(false));
  }

  private void assertDomainAnchorFileDoesNotExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(false));
  }

  private void assertDomainAnchorFileExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(true));
  }

  private File getArtifactAnchorFile(String artifactName, File artifactDir) {
    String anchorFileName = artifactName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
    return new File(artifactDir, anchorFileName);
  }

  private void assertAppFolderIsDeleted(String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(appsDir, appName);
    prober.check(new FileDoesNotExists(appFolder));
  }

  private void assertAppFolderIsMaintained(String appName) {
    assetArtifactFolderIsMaintained(appName, appsDir);
  }

  private void assertDomainFolderIsMaintained(String domainName) {
    assetArtifactFolderIsMaintained(domainName, domainsDir);
  }

  private void assetArtifactFolderIsMaintained(String artifactName, File artifactDir) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(artifactDir, artifactName);
    prober.check(new FileExists(appFolder));
  }

  private void executeApplicationFlow(String flowName) throws MuleException {
    Flow mainFlow =
        (Flow) deploymentService.getApplications().get(0).getMuleContext().getRegistry().lookupFlowConstruct(flowName);
    Message muleMessage = of(TEST_MESSAGE);

    mainFlow.process(Event.builder(DefaultEventContext.create(mainFlow, TEST_CONNECTOR_LOCATION))
        .message(muleMessage)
        .flow(mainFlow)
        .build());
  }

  private void assertZombieApplication(String appId) {
    final Map<URI, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URI, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", appId, new File(zombie.getKey()).getParentFile().getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  /**
   * Allows to execute custom actions before or after executing logic or checking preconditions / verifications.
   */
  private interface Action {

    void perform() throws Exception;
  }

  private static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                           String bundleDescriptorLoaderId) {
    return createBundleDescriptorLoader(artifactId, classifier, bundleDescriptorLoaderId, "1.0");
  }

  private static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                           String bundleDescriptorLoaderId, String version) {
    Map<String, Object> attributes = new HashMap();
    attributes.put(VERSION, version);
    attributes.put(GROUP_ID, "org.mule.test");
    attributes.put(ARTIFACT_ID, artifactId);
    attributes.put(CLASSIFIER, classifier);
    attributes.put(TYPE, EXTENSION_BUNDLE_TYPE);

    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

  public static class WaitComponent implements Initialisable {

    public static Latch componentInitializedLatch = new Latch();
    public static Latch waitLatch = new Latch();

    @Override
    public void initialise() throws InitialisationException {
      try {
        componentInitializedLatch.release();
        waitLatch.await();
      } catch (InterruptedException e) {
        throw new InitialisationException(e, this);
      }
    }

    public static void reset() {
      componentInitializedLatch = new Latch();
      waitLatch = new Latch();
    }
  }

  /**
   * Component used on deployment test that require policies to check that they are invoked
   * <p/>
   * Static state must be reset before each test is executed
   */
  public static class TestPolicyProcessor implements org.mule.runtime.core.api.processor.Processor {

    public static volatile int invocationCount;
    public static volatile String policyParametrization = "";

    @Override
    public Event process(Event event) throws MuleException {
      invocationCount++;
      String variableName = "policyParameter";
      if (event.getVariables().keySet().contains(variableName)) {
        policyParametrization += event.getVariables().get(variableName).getValue();
      }

      return event;
    }
  }

  private void deployAfterStartUp(ApplicationFileBuilder applicationFileBuilder) throws Exception {
    startDeployment();

    addPackedAppFromBuilder(applicationFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertAppsDir(NONE, new String[] {applicationFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(applicationFileBuilder.getId());
  }

  private PolicyFileBuilder createPolicyIncludingHelloPluginV2FileBuilder() {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    mulePolicyModelBuilder.withClassLoaderModelDescriber().setId(MAVEN);
    return new PolicyFileBuilder(BAZ_POLICY_NAME)
        .describedBy(mulePolicyModelBuilder.build())
        .dependingOn(helloExtensionV2Plugin);
  }

  private ArtifactPluginFileBuilder createHelloExtensionV2PluginFileBuilder() {
    MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin")
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_EXTENSION_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "2.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriber()
        .addProperty(EXPORTED_RESOURCES,
                     asList("/", "META-INF/mule-hello.xsd", "META-INF/spring.handlers", "META-INF/spring.schemas"))
        .setId(MAVEN);
    return new ArtifactPluginFileBuilder("helloExtensionPlugin-2.0")
        .dependingOn(new JarFileBuilder("helloExtensionV2", helloExtensionV2JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private ArtifactPluginFileBuilder createHelloExtensionV1PluginFileBuilder() {
    MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin")
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_EXTENSION_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriber().setId(MAVEN)
        .addProperty(EXPORTED_RESOURCES,
                     asList("/", "META-INF/mule-hello.xsd", "META-INF/spring.handlers", "META-INF/spring.schemas"));
    return new ArtifactPluginFileBuilder("helloExtensionPlugin-1.0")
        .dependingOn(new JarFileBuilder("helloExtensionV1", helloExtensionV1JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private PolicyFileBuilder createPolicyIncludingPluginFileBuilder() {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    mulePolicyModelBuilder.withClassLoaderModelDescriber().setId(MAVEN);
    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build()).dependingOn(helloExtensionV1Plugin);
  }
}
