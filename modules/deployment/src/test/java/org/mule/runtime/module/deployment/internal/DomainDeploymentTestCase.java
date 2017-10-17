/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.touch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.internal.TestPolicyProcessor.invocationCount;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test for domain deployment
 */
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
  public void deploysDomainWithSharedLibPrecedenceOverApplicationSharedLib() throws Exception {
    final String domainId = "shared-lib";
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml");
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("shared-lib-precedence-app")
        .definedBy("app-shared-lib-precedence-config.xml")
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
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
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
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
        new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
            .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).dependingOnSharedLibrary(new JarFileBuilder("barUtils1.0", barUtils1_0JarFile))
            .definedBy("empty-domain-config.xml");

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .dependingOn(pluginFileBuilder).dependingOn(domainFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getRegistry());
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
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder).dependingOn(dummyDomainBundleFileBuilder));

    startDeployment();

    deploysDomain();
  }

  @Test
  public void deploysDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);
    addPackedAppFromBuilder(
                            new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
                                .dependingOn(dummyDomainBundleFileBuilder));

    startDeployment();

    deploysDomain();
  }

  @Test
  public void deploysDomainBundleZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);
    addPackedAppFromBuilder(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(dummyDomainBundleFileBuilder));

    deploysDomain();
  }

  private void deploysDomain() {
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainBundleFileBuilder.getId()}, true);

    final Domain domain = findADomain(dummyDomainBundleFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getRegistry());

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
    assertNotNull(domain.getRegistry());
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
            .dependingOn(domainFileBuilder);


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

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
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

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

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
    assertThat(domain.getRegistry().lookupByName("http-listener-config").isPresent(), is(true));

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

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
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

    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
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
    copyFile(newConfigFile, originalConfigFile);
    assertFailedDomainRedeploymentSuccess(incompleteDomainFileBuilder.getId());

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
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
    deploymentService.deployDomain(uri, deploymentProperties);
  }

  @Override
  protected void redeployId(String id, Properties deploymentProperties) throws IOException {
    if (deploymentProperties == null) {
      deploymentService.redeployDomain(id);
    } else {
      deploymentService.redeployDomain(id, deploymentProperties);
    }
  }
}
