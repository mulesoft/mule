/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.CREATED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.builder.DeployableArtifactClassLoaderFactoryProvider.domainClassLoaderFactory;
import static org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtils1ClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtils1_0JarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtils2_0JarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtilsForbiddenJavaJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtilsForbiddenMuleContainerJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.barUtilsForbiddenMuleThirdPartyJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionCustomException;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionLoadingResource;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionPlusPlugin1Echo;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionPlusPlugin2Echo;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.classloaderConfigConnectExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.classloaderConnectExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.goodbyeExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV2Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.loadClassExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.loadsAppResourceCallbackJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginEcho1ClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginEcho3ClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginForbiddenJavaEchoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginForbiddenMuleContainerEchoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginForbiddenMuleThirdPartyEchoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestDomainFactory.createDomainFactory;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.deploy;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.deployDomain;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.redeployDomain;
import static org.mule.runtime.module.deployment.test.internal.util.Utils.getResourceFile;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.touch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.internal.DeploymentStatusTracker;
import org.mule.tck.probe.PollingProber;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test for domain deployment
 */
@Feature(DOMAIN_DEPLOYMENT)
public class DomainDeploymentTestCase extends AbstractDeploymentTestCase {

  // Domain artifacts builders
  private final DomainFileBuilder brokenDomainFileBuilder = new DomainFileBuilder("brokenDomain").corrupted();
  private final DomainFileBuilder emptyDomainFileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml");
  private final DomainFileBuilder waitDomainFileBuilder =
      new DomainFileBuilder("wait-domain").definedBy("wait-domain-config.xml");
  private final DomainFileBuilder incompleteDomainFileBuilder =
      new DomainFileBuilder("incompleteDomain").definedBy("incomplete-domain-config.xml");
  private final DomainFileBuilder invalidDomainBundleFileBuilder =
      new DomainFileBuilder("invalid-domain-bundle").definedBy("incomplete-domain-config.xml");
  private final DomainFileBuilder dummyDomainBundleFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
      .definedBy("empty-domain-config.xml");
  private final DomainFileBuilder dummyUndeployableDomainFileBuilder = new DomainFileBuilder("dummy-undeployable-domain")
      .definedBy("empty-domain-config.xml").deployedWith("redeployment.enabled", "false");
  private final DomainFileBuilder sharedDomainFileBuilder =
      new DomainFileBuilder("shared-domain").definedBy("shared-domain-config.xml");
  private final DomainFileBuilder domainWithPropsFileBuilder =
      new DomainFileBuilder("domain-with-props").definedBy("domain-with-props-config.xml");

  private final DomainFileBuilder emptyDomain100FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.0");
  private final DomainFileBuilder emptyDomain101FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.1");

  // Application artifact builders
  private final ApplicationFileBuilder dummyDomainApp1FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app1").definedBy("empty-config.xml").dependingOn(dummyDomainFileBuilder);
  private final ApplicationFileBuilder dummyDomainApp2FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app2").definedBy("empty-config.xml").dependingOn(dummyDomainFileBuilder);
  private final ApplicationFileBuilder dummyDomainApp3FileBuilder = new ApplicationFileBuilder("dummy-domain-app3")
      .definedBy("bad-app-config.xml").dependingOn(dummyDomainFileBuilder);
  private final ApplicationFileBuilder sharedAAppFileBuilder = new ApplicationFileBuilder("shared-app-a")
      .definedBy("shared-a-app-config.xml").dependingOn(sharedDomainFileBuilder);
  private final ApplicationFileBuilder sharedBAppFileBuilder = new ApplicationFileBuilder("shared-app-b")
      .definedBy("shared-b-app-config.xml").dependingOn(sharedDomainFileBuilder);

  public DomainDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @After
  public void disposeStaleDomains() {
    TestDomainFactory.after();
  }

  @Test
  @Ignore("MULE-12255 Add the test plugin as a plugin of the domain")
  public void redeployModifiedDomainAndRedeployFailedApps() throws Exception {
    DomainFileBuilder sharedBundleDomainFileBuilder = new DomainFileBuilder("shared-domain")
        .definedBy("shared-domain-config.xml");
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
  public void deploysTwoIdenticalDomainsWithDifferentNames() throws Exception {
    String aDomainName = emptyDomainFileBuilder.getId() + "A";
    String anotherDomainName = emptyDomainFileBuilder.getId() + "B";

    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, aDomainName);
    assertDeploymentSuccess(domainDeploymentListener, aDomainName);

    reset(domainDeploymentListener);

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, anotherDomainName);
    assertDeploymentSuccess(domainDeploymentListener, anotherDomainName);
  }

  @Test
  public void deployTwoCompatibleDomains() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationSharedLib() throws Exception {
    final String domainId = "shared-lib";
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml");
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("shared-lib-precedence-app")
        .definedBy("app-shared-lib-precedence-config.xml")
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .dependingOn(callbackExtensionPlusPlugin1Echo)
        .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationLib() throws Exception {
    final String domainId = "shared-lib";
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1_0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml");
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOnSharedLibrary(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
            .dependingOn(callbackExtensionPlusPlugin1Echo)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationPluginLib() throws Exception {
    final String domainId = "shared-lib";
    final ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
            .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class")
            .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1.0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml");

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(pluginFileBuilder)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-17112")
  @Description("If a plugin uses a library and the domain sets another version of that library as a sharedLib, the plugin internally uses its own version of the lib and not the domain's.")
  public void pluginWithDependencyAndConflictingVersionSharedByApp() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class");

    final String domainId = "shared-lib";
    final DomainFileBuilder domainFileBuilder = new DomainFileBuilder(domainId)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
        .definedBy("empty-domain-config.xml");

    final ApplicationFileBuilder differentLibPluginAppFileBuilder =
        new ApplicationFileBuilder("appInDomainWithLibDifferentThanPlugin")
            .definedBy("app-plugin-different-lib-config.xml")
            .dependingOn(echoPluginWithLib1)
            .dependingOn(domainFileBuilder)
            .dependingOn(callbackExtensionPlusPlugin2Echo);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-17593")
  @Description("The IBM CTG connector must be prevented to use the fix in MULE-17112.")
  public void denylistedPluginWithDependencyAndConflictingVersionSharedByApp() throws Exception {
    ArtifactPluginFileBuilder echoPluginWithLib1 = new ArtifactPluginFileBuilder("mule-ibm-ctg-connector")
        .withGroupId("com.mulesoft.connectors").withVersion("2.3.1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class");

    final String domainId = "shared-lib";
    final DomainFileBuilder domainFileBuilder = new DomainFileBuilder(domainId)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
        .definedBy("empty-domain-config.xml");

    final ApplicationFileBuilder differentLibPluginAppFileBuilder =
        new ApplicationFileBuilder("appInDomainWithLibDifferentThanPlugin")
            .definedBy("app-plugin-different-lib-config.xml")
            .dependingOn(echoPluginWithLib1)
            .dependingOn(domainFileBuilder)
            .dependingOn(callbackExtensionPlusPlugin2Echo);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    try {
      executeApplicationFlow("main");
      fail("Flow should throw an exception which original cause is a NoSuchMethodError");
    } catch (Throwable caught) {
      Throwable originalCause = getOriginalCause(caught);
      assertThat(originalCause, instanceOf(NoSuchMethodError.class));
      assertThat(originalCause.getMessage(), containsString("BarUtils.doStuff"));
    }
  }

  private static Throwable getOriginalCause(Throwable exception) {
    if (exception.getCause() == null) {
      return exception;
    }

    if (exception.getCause() == exception) {
      return exception;
    }

    return getOriginalCause(exception.getCause());
  }

  @Test
  public void pluginFromDomainUsedInApp() throws Exception {
    addPackedDomainFromBuilder(exceptionThrowingPluginImportingDomain);

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices("exception-throwing-app.xml")
            .dependingOn(callbackExtensionCustomException);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();
    triggerDirectoryWatcher();

    try {
      executeApplicationFlow("main");
      fail("Flow execution was expected to throw an exception");
    } catch (MuleRuntimeException expected) {
      assertThat(expected.getCause().getCause().getClass().getName(), is(equalTo("org.exception.CustomException")));
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
    assertNotNull(domain.getArtifactContext().getRegistry());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void memoryManagementCanBeInjectedInDomain() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    InjectedMemoryManagement injectedMemoryManagement = new InjectedMemoryManagement();
    domain.getArtifactContext().getMuleContext().getInjector().inject(injectedMemoryManagement);

    assertThat(injectedMemoryManagement.getMemoryManagementService(), is(notNullValue()));
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
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dummyDomainBundleFileBuilder));

    startDeployment();

    deploysDomain();
  }

  @Test
  public void deploysDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dummyDomainBundleFileBuilder));

    startDeployment();

    deploysDomain();
  }

  @Test
  public void deploysDomainBundleZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dummyDomainBundleFileBuilder));

    triggerDirectoryWatcher();

    deploysDomain();
  }

  private void deploysDomain() {
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainBundleFileBuilder.getId()}, true);

    final Domain domain = findADomain(dummyDomainBundleFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getArtifactContext().getRegistry());

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
    assertNotNull(domain.getArtifactContext().getRegistry());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysBrokenDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenDomainFileBuilder.getDeployedPath(), deploymentService.getZombieDomains());
  }

  @Test
  public void deploysBrokenDomainZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenDomainFileBuilder.getDeployedPath(), deploymentService.getZombieDomains());
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

    assertDomainRedeploymentSuccess(emptyDomainFileBuilder.getId());
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

    assertDomainRedeploymentSuccess(emptyDomainFileBuilder.getId());
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
    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
  }

  @Test
  @Issue("MULE-19040")
  @Description("When a domain was stopped and the server is restarted, the domain should not start")
  public void redeploysDomainZipRefreshesAppsButIfTheyWereStoppedTheyDoNotStartAndNoStatusPersistenceWasSaved() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    final Application app = findApp(dummyDomainApp1FileBuilder.getId(), 1);
    app.stop();

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertStatus(dummyDomainApp1FileBuilder.getId(), CREATED);

    Optional<Properties> deploymentProperties =
        getPersistedArtifactStatusDeploymentProperties(dummyDomainApp1FileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Test
  @Issue("MULE-19890")
  public void redeploysDomainZipRefreshesAppsAndStartsThemAndNoStatusPersistenceWasSaved() throws Exception {
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
    assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertStatus(dummyDomainApp1FileBuilder.getId(), STARTED);

    Optional<Properties> artifactStatusDeploymentProperties =
        getPersistedArtifactStatusDeploymentProperties(dummyDomainApp1FileBuilder.getId());
    assertThat(artifactStatusDeploymentProperties.isPresent(), is(true));
    assertThat(artifactStatusDeploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Test
  @Issue("W-10984029")
  public void redeploysDomainZipRefreshesAppsAndStartsThemAndDeploymentPropertiesAreNotErased() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    startDeployment();
    triggerDirectoryWatcher();

    Properties initialDeploymentProperties = new Properties();
    initialDeploymentProperties.put(COMPONENT_NAME, COMPONENT_CLASS);
    deploy(deploymentService, dummyDomainApp1FileBuilder.getArtifactFile().getAbsoluteFile().toURI(),
           initialDeploymentProperties);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertStatus(dummyDomainApp1FileBuilder.getId(), STARTED);

    Optional<Properties> finalDeploymentProperties = getPersistedDeploymentProperties(dummyDomainApp1FileBuilder.getId());
    assertThat(finalDeploymentProperties.isPresent(), is(true));
    assertThat(finalDeploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
    assertThat(finalDeploymentProperties.get().get(COMPONENT_NAME), is(COMPONENT_CLASS));
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

    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
  }

  @Test
  public void deploysAppUsingDomainPlugin() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(echoPlugin);

    ApplicationFileBuilder echoPluginAppFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-14131")
  @Description("Plugin as dependency in domain and app")
  public void deploysAppAndDomainWithSamePluginDependency() throws Exception {

    final String domainId = "shared-lib";
    final ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
            .containingClass(pluginEcho1ClassFile, "org/foo/Plugin1Echo.class")
            .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1.0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml").dependingOn(pluginFileBuilder);

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(pluginFileBuilder)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingDomainPluginThatLoadsAppResource() throws Exception {
    ArtifactPluginFileBuilder loadsAppResourceCallbackPlugin = new ArtifactPluginFileBuilder("loadsAppResourceCallbackPlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("loadsAppResourceCallbackJar", loadsAppResourceCallbackJarFile));

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("loading-domain")
        .definedBy("empty-domain-config.xml")
        .dependingOn(loadsAppResourceCallbackPlugin);

    ApplicationFileBuilder nonExposingAppFileBuilder = new ApplicationFileBuilder("exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-loads-app-resource-plugin-config.xml")
        .dependingOn(callbackExtensionLoadingResource)
        .containingClass(barUtils1ClassFile, "org/bar/BarUtils.class")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt");

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppWithPluginDependingOnDomainPlugin() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(echoPlugin);

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3ClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    ApplicationFileBuilder echoPluginAppFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(domainFileBuilder).dependingOn(dependantPlugin);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingDomainExtension() throws Exception {
    installEchoService();
    installFooService();

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("appWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingDomainExtensionWithSharedExtension() throws Exception {
    installEchoService();
    installFooService();

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("hello-domain-bundle")
        .definedBy("hello-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("appWithSharedHelloExtension").definedBy(APP_WITH_SHARED_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void failsToDeployAppWithDomainPluginVersionMismatch() throws Exception {
    installEchoService();
    installFooService();

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder)
            .dependingOn(helloExtensionV2Plugin);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeployDomainWithPluginThatUsesExtensionsClient() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("goodbye-domain-config.xml")
        .dependingOn(goodbyeExtensionV1Plugin);

    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
  }

  @Test
  public void appliesApplicationPolicyUsingDomainPlugin() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);
  }

  @Test
  public void appliesApplicationPolicyWithPluginDependingOnDomainPlugin() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingDependantPluginFileBuilder.getArtifactFile());

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);
  }

  @Test
  public void appliesApplicationPolicyDuplicatingDomainPlugin() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);


    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);
  }

  @Ignore("MULE-15842: fix once we support declaring share objects plugins in policies")
  @Test
  public void failsToApplyApplicationPolicyWithDomainPluginVersionMismatch() throws Exception {
    installEchoService();
    installFooService();

    policyManager.registerPolicyTemplate(policyIncludingHelloPluginV2FileBuilder.getArtifactFile());

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("empty-domain-config.xml")
        .dependingOn(helloExtensionV1Plugin);

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithHelloExtension").definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG)
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingHelloPluginV2FileBuilder.getArtifactId(),
                              new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                        getResourceFile("/appPluginPolicy.xml"), emptyList()));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
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
    reset(domainDeploymentListener);

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
    assertArtifactIsRegisteredAsZombie("domain with spaces", deploymentService.getZombieDomains());
  }

  @Test
  public void deploysInvalidExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, "domain with spaces"}, true);
    assertArtifactIsRegisteredAsZombie("domain with spaces", deploymentService.getZombieDomains());
  }

  @Test
  public void deploysInvalidExplodedDomainOnlyOnce() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");
    assertDeploymentFailure(domainDeploymentListener, "domain with spaces", times(1));

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    DomainFileBuilder updatedDomainFileBuilder = new DomainFileBuilder("empty2-domain", emptyDomainFileBuilder);
    addExplodedDomainFromBuilder(updatedDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, updatedDomainFileBuilder.getId());

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
    assertArtifactIsRegisteredAsZombie(incompleteDomainFileBuilder.getId(), deploymentService.getZombieDomains());
  }

  @Test
  public void deploysBrokenExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, incompleteDomainFileBuilder.getId()}, true);
    assertArtifactIsRegisteredAsZombie(incompleteDomainFileBuilder.getId(), deploymentService.getZombieDomains());
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
  public void undeploysStoppedDomainAndDoesNotPersistStatus() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    domain.stop();

    deploymentService.undeploy(domain);
    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyDomainFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Test
  @Issue("MULE-19040")
  @Description("When a domain was stopped, this state should be persisted as a deployment property")
  public void whenDomainIsStoppedStateIsPersistedAsDeploymentProperty() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    domain.stop();

    assertThat(domain.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));

    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyDomainFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));
  }

  @Test
  @Issue("MULE-19040")
  @Description("When a domain was stopped by undeployment, this state should not be persisted as a deployment property")
  public void whenDomainIsStoppedByUndeploymentStateIsNotPersistedAsDeploymentProperty() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    final Domain domain = findADomain(emptyDomainFileBuilder.getId());

    assertThat(domain.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));
    deploymentService.undeploy(domain);

    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyDomainFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
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

    TestDomainFactory testDomainFactory = createDomainFactory(domainClassLoaderFactory(name -> getAppDataFolder(name)),
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

    TestDomainFactory testDomainFactory = createDomainFactory(domainClassLoaderFactory(name -> getAppDataFolder(name)),
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
    assertArtifactIsRegisteredAsZombie(incompleteDomainFileBuilder.getId(), deploymentService.getZombieDomains());
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
    assertArtifactIsRegisteredAsZombie(incompleteDomainFileBuilder.getId(), deploymentService.getZombieDomains());
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
    assertArtifactIsRegisteredAsZombie(incompleteDomainFileBuilder.getId(), deploymentService.getZombieDomains());
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
    assertFailedDomainRedeploymentSuccess(incompleteDomainFileBuilder.getId());
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

    assertFailedDomainRedeploymentSuccess(incompleteDomainFileBuilder.getId());
  }

  @Test
  public void deployAndRedeployDomainWithDeploymentProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(COMPONENT_NAME, COMPONENT_CLASS);
    startDeployment();
    triggerDirectoryWatcher();
    deployAndVerifyPropertyInRegistry(domainWithPropsFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName(COMPONENT_NAME_IN_APP).get() instanceof TestComponent);


    // Redeploys without deployment properties (remains the same, as it takes the deployment properties from the persisted file)
    redeployAndVerifyPropertyInRegistry(domainWithPropsFileBuilder.getId(), null,
                                        (registry) -> registry.lookupByName(COMPONENT_NAME_IN_APP)
                                            .get() instanceof TestComponent);


    // Redeploy with new deployment properties
    deploymentProperties.clear();
    deploymentProperties.put(COMPONENT_NAME, COMPONENT_CLASS_ON_REDEPLOY);
    redeployAndVerifyPropertyInRegistry(domainWithPropsFileBuilder.getId(), deploymentProperties,
                                        (registry) -> registry.lookupByName(COMPONENT_NAME_IN_APP)
                                            .get() instanceof TestComponentOnRedeploy);
  }

  @Test
  @Issue("MULE-19040")
  @Description("When a domain is restarted, if its apps were stopped before restart, they should not get started")
  public void redeployDomainWithStoppedAppsShouldNotPersistStoppedStateAndShouldNotStartApps() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);
    addPackedDomainFromBuilder(dummyDomainFileBuilder);

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    final Domain domain = findADomain(dummyDomainFileBuilder.getId());
    assertThat(domain.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    final Application app = findApp(dummyDomainApp1FileBuilder.getId(), 1);
    app.stop();

    assertStatus(dummyDomainApp1FileBuilder.getId(), STOPPED);

    redeployId(dummyDomainFileBuilder.getId(), null);

    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(dummyDomainApp1FileBuilder.getId());
    assertStatus(dummyDomainApp1FileBuilder.getId(), CREATED);

    Optional<Properties> deploymentProperties =
        getPersistedArtifactStatusDeploymentProperties(dummyDomainApp1FileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
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
    assertThat(domain.getArtifactContext().getRegistry().lookupByName("http-listener-config").isPresent(), is(true));

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

    assertDomainRedeploymentFailure(emptyDomainFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());
    assertDomainRedeploymentFailure(emptyDomainFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertFailedDomainRedeploymentFailure(incompleteDomainFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertFailedDomainRedeploymentFailure(incompleteDomainFileBuilder.getId());
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

    assertFailedDomainRedeploymentSuccess(incompleteDomainFileBuilder.getId());
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

    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());

    assertApplicationRedeploymentSuccess(dummyDomainApp1FileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void redeploysDomainAndAllApplicationsEvenWhenOneFails() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    deploymentService.getLock().lock();
    try {
      doRedeployDummyDomainByChangingConfigFileWithGoodOne();
      ApplicationFileBuilder updateAppDomainBuilder =
          new ApplicationFileBuilder("dummy-domain-app1").definedBy("incomplete-app-config.xml");
      addExplodedAppFromBuilder(updateAppDomainBuilder);
    } finally {
      deploymentService.getLock().unlock();
    }

    assertDomainRedeploymentFailure(dummyDomainFileBuilder.getId());
    assertApplicationRedeploymentFailure(dummyDomainApp1FileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void doesNotRedeployDomainWithRedeploymentDisabled() throws Exception {
    addExplodedDomainFromBuilder(dummyUndeployableDomainFileBuilder, dummyUndeployableDomainFileBuilder.getId());
    addPackedAppFromBuilder(emptyAppFileBuilder);

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

    assertDomainRedeploymentFailure(dummyDomainFileBuilder.getId());

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

    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());

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

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertDomainRedeploymentFailure(dummyDomainFileBuilder.getId());
  }

  @Test
  public void deployFailsWhenMissingFile() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    File originalConfigFile =
        new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
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
  public void applicationBundledWithinDomainNotRemovedAfterFullDeploy()
      throws Exception {
    resetUndeployLatch();
    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(dummyDomainBundleFileBuilder));
    startDeployment();
    deploysDomain();

    doRedeployBrokenDomainAfterFixedDomain();
  }

  @Test
  public void domainIncludingForbiddenJavaClass() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("forbidden-domain")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .definedBy("empty-domain-config.xml")
        .containingClass(pluginForbiddenJavaEchoTestClassFile, "org/foo/echo/PluginForbiddenJavaEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenJavaJarFile", barUtilsForbiddenJavaJarFile));

    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-java-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
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
  public void domainIncludingForbiddenMuleContainerClass() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("forbidden-domain")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .definedBy("empty-domain-config.xml")
        .containingClass(pluginForbiddenMuleContainerEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleContainerEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleContainerJarFile", barUtilsForbiddenMuleContainerJarFile));

    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-mule-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
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
  public void domainIncludingForbiddenMuleContainerThirdParty() throws Exception {
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("forbidden-domain")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .definedBy("empty-domain-config.xml")
        .containingClass(pluginForbiddenMuleThirdPartyEchoTestClassFile, "org/foo/echo/PluginForbiddenMuleThirdPartyEcho.class")
        .dependingOn(new JarFileBuilder("barUtilsForbiddenMuleThirdPartyJarFile", barUtilsForbiddenMuleThirdPartyJarFile));

    final ApplicationFileBuilder forbidden = appFileBuilder("forbidden")
        .definedBy("app-with-forbidden-mule3rd-echo-plugin-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
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
  @Issue("MULE-18159")
  public void pluginDeclaredInDomainIsAbleToLoadClassesExportedByTheAppWhereItIsUsed() throws Exception {
    // Given a plugin which loads classes.
    final ArtifactPluginFileBuilder pluginWhichLoadsClasses = loadClassExtensionPlugin;

    // Given a domain depending on the plugin.
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("domain-with-test-plugin")
        .definedBy("empty-domain-config.xml")
        .dependingOn(pluginWhichLoadsClasses);

    // Given an app depending on the domain and exporting a class.
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("app-with-load-class-operation").definedBy("app-with-load-class-operation.xml")
            .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
            .configuredWith(EXPORTED_PACKAGES, "org.foo")
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    // When the app uses the plugin in order to load the exported class, then it doesn't raise any error.
    executeApplicationFlow("flowWhichTriesToLoadTheClass");
  }

  @Test
  @Issue("MULE-18159")
  public void pluginDeclaredInDomainIsAbleToLoadClassesExportedByTheAppWhereItIsUsedOnNonBlockingCompletion() throws Exception {
    // Given a plugin which loads classes.
    final ArtifactPluginFileBuilder pluginWhichLoadsClasses = loadClassExtensionPlugin;

    // Given a domain depending on the plugin.
    DomainFileBuilder domainFileBuilder = new DomainFileBuilder("domain-with-test-plugin")
        .definedBy("empty-domain-config.xml")
        .dependingOn(pluginWhichLoadsClasses);

    // Given an app depending on the domain and exporting a class.
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("app-with-load-class-operation").definedBy("app-with-load-class-operation.xml")
            .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
            .configuredWith(EXPORTED_PACKAGES, "org.foo")
            .dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    ExecutorService executor = newSingleThreadExecutor();

    // When the app uses the plugin in order to load the exported class on non-blocking completion (within error handler),
    // then it doesn't raise any error.
    executor.submit(() -> {
      try {
        executeApplicationFlow("flowWhichSavesTheCallbackAndLoadsClassesInErrorHandler");
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });

    CompletionCallback<Object, Object> completionCallback = getCompletionCallback("SavedCallback");
    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> completionCallback.error(new NullPointerException()));
  }

  @Test
  @Issue("MULE-19376")
  @Description("When both the app as the extension share a resource with the same name, the runtime should choose the extension's when the resource is obtained at an operation")
  public void pluginDeclaredInDomainIsAbleToGetResourceWithSameNameInAppAndExtensionFromExtension() throws Exception {
    String resourceFileName = "file.txt";

    final ApplicationFileBuilder applicationFileBuilder =
        getApplicationWithResourceFileBuilder(classloaderConnectExtensionPlugin, "app-with-connection", resourceFileName);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    assertObtainedResourceIsCorrect("/org/foo/connection/extension/" + resourceFileName, "flowWhichConnects");
  }

  @Test
  @Issue("MULE-19376")
  @Description("When both the app as the extension share a resource with the same name, the runtime should choose the extension's when the resource is obtained at config")
  public void pluginDeclaredInDomainIsAbleToGetResourceWithSameNameInAppAndExtensionFromExtensionInConfig() throws Exception {
    String resourceFileName = "file.txt";

    final ApplicationFileBuilder applicationFileBuilder =
        getApplicationWithResourceFileBuilder(classloaderConfigConnectExtensionPlugin, "app-with-config-connection",
                                              resourceFileName);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    assertObtainedResourceIsCorrect("/org/foo/connection/extension/" + resourceFileName, "flowWhichConnects");
  }

  private void assertObtainedResourceIsCorrect(String correctResourceLocation, String flowName) throws Exception {
    File resourceFile = getResourceFile(correctResourceLocation);
    BufferedReader reader = Files.newBufferedReader(resourceFile.toPath(), defaultCharset());

    CoreEvent result = executeApplicationFlow(flowName, null);
    assertThat(result.getMessage().getPayload().getValue(), is(equalTo(reader.readLine())));
  }

  private ApplicationFileBuilder getApplicationWithResourceFileBuilder(ArtifactPluginFileBuilder classloaderConfigConnectExtensionPlugin,
                                                                       String appName, String resourceName) {
    // Given a plugin which creates a connection.
    final ArtifactPluginFileBuilder pluginWhichCreatesConnection = classloaderConfigConnectExtensionPlugin;

    return new ApplicationFileBuilder(appName).definedBy(appName + ".xml")
        .dependingOn(pluginWhichCreatesConnection)
        .containingResource(resourceName, resourceName);
  }

  private CompletionCallback<Object, Object> getCompletionCallback(String callbackName) {
    Registry registry = deploymentService.getApplications().get(0).getArtifactContext().getRegistry();
    Map<String, CompletionCallback<Object, Object>> callbacksMap =
        (Map<String, CompletionCallback<Object, Object>>) registry.lookupByName("completion.callbacks").get();
    PollingProber.probe(() -> callbacksMap.containsKey(callbackName));
    return callbacksMap.get(callbackName);
  }

  protected ApplicationFileBuilder appFileBuilder(final String artifactId) {
    return new ApplicationFileBuilder(artifactId);
  }

  private void doSynchronizedDomainDeploymentActionTest(final Action deploymentAction, final Action assertAction)
      throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    final DeploymentListener domainDeploymentListener = this.domainDeploymentListener;
    final String artifactId = emptyDomainFileBuilder.getId();

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, domainDeploymentListener, artifactId);
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
        new File(new File(domainsDir, domain.getDeployedPath()), getConfigFilePathWithinArtifact(domain.getConfigFile()));
    assertThat("Cannot find domain config file: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  private void doRedeployFixedDomainAfterBrokenDomain() throws Exception {
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    File originalConfigFile =
        new File(domainsDir + File.separator + incompleteDomainFileBuilder.getId(), DEFAULT_CONFIGURATION_RESOURCE);
    URL url = getClass().getResource("/empty-domain-config.xml");
    File newConfigFile = new File(url.toURI());
    updateFileModifiedTime(originalConfigFile.lastModified(), newConfigFile);
    copyFile(newConfigFile, originalConfigFile);
    assertFailedDomainRedeploymentSuccess(incompleteDomainFileBuilder.getId());

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
  }

  /**
   * After a successful deploy using the {@link DomainDeploymentTestCase#domainDeploymentListener}, this method deploys a domain
   * zip with the same name and a wrong configuration. Applications dependant of the domain should not be deleted after this
   * failure full redeploy.
   */
  private void doRedeployBrokenDomainAfterFixedDomain() throws Exception {
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    reset(domainDeploymentListener);

    DomainFileBuilder domainBundleWrongFullRedeploy = new DomainFileBuilder("dummy-domain-bundle")
        .definedBy("incomplete-domain-config.xml");

    addPackedDomainFromBuilder(domainBundleWrongFullRedeploy);

    assertDeploymentFailure(domainDeploymentListener, domainBundleWrongFullRedeploy.getId());

    assertThat(undeployLatch.await(5000, SECONDS), is(true));

    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());
    Application dependantApplication = deploymentService.getApplications().get(0);
    assertThat(dependantApplication, is(notNullValue()));
    assertThat(dependantApplication.getStatus(), is(DESTROYED));
  }

  @Test
  public void domainWithNonExistentConfigResourceOnDeclaration() throws Exception {
    DomainFileBuilder domainBundleNonExistentConfigResource = new DomainFileBuilder("non-existent-domain-config-resource")
        .definedBy("empty-domain-config.xml").deployedWith(PROPERTY_CONFIG_RESOURCES, "mule-non-existent-config.xml");

    addPackedDomainFromBuilder(domainBundleNonExistentConfigResource);
    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, domainBundleNonExistentConfigResource.getId());
  }

  private void deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExists = () -> assertDomainAnchorFileDoesNotExists(waitDomainFileBuilder.getId());
    Action verifyDeploymentSuccessful = () -> assertDeploymentSuccess(domainDeploymentListener, waitDomainFileBuilder.getId());
    Action verifyAnchorFileExists = () -> assertDomainAnchorFileExists(waitDomainFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExists,
                                                                verifyDeploymentSuccessful, verifyAnchorFileExists);
  }

  @Override
  protected void deployURI(URI uri, Properties deploymentProperties) throws IOException {
    deployDomain(deploymentService, uri, deploymentProperties);
  }

  @Override
  protected void redeployId(String id, Properties deploymentProperties) throws IOException {
    if (deploymentProperties == null) {
      redeployDomain(deploymentService, id);
    } else {
      redeployDomain(deploymentService, id, deploymentProperties);
    }
  }

  /**
   * Class to test injection of memory management.
   */
  private static class InjectedMemoryManagement {

    @Inject
    private MemoryManagementService memoryManagementService;

    public MemoryManagementService getMemoryManagementService() {
      return memoryManagementService;
    }
  }

}
