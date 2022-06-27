/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_POLICY_ISOLATION_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.NULL_CLASSLOADER_MODEL;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentSuccessfulStory.POLICY_ISOLATION;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionDiscoveryRequest;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelDiscoverer;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderSupplier;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.policy.api.PolicyPointcut;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(POLICY_DEPLOYMENT)
@Story(POLICY_ISOLATION)
@RunWith(Parameterized.class)
public class DefaultApplicationPolicyInstanceTestCase extends AbstractMuleTestCase {

  @Parameters(name = "enablePolicyIsolation: {0}")
  public static Boolean[] parameters() {
    return new Boolean[] {false, true};
  }

  @Rule
  public SystemProperty enablePolicyIsolation;

  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);

  private PolicyTemplate policyTemplate;
  private Application application;
  private Registry appRegistry;

  private MuleContextWithRegistry policyMuleContext;
  private MuleRegistry policyRegistry;

  public DefaultApplicationPolicyInstanceTestCase(boolean enablePolicyIsolationPropertyValue) {
    this.enablePolicyIsolation = new SystemProperty(ENABLE_POLICY_ISOLATION_PROPERTY, TEST_PAYLOAD);
  }

  @Before
  public void setUp() throws RegistrationException {
    policyTemplate = mock(PolicyTemplate.class, RETURNS_DEEP_STUBS);
    when(policyTemplate.getArtifactClassLoader().getClassLoader()).thenReturn(this.getClass().getClassLoader());
    when(policyTemplate.getDescriptor()).thenReturn(new PolicyTemplateDescriptor("policyId"));

    application = mock(Application.class, RETURNS_DEEP_STUBS);

    ArtifactContext appArtifactContext = application.getArtifactContext();
    MuleContextWithRegistry appMuleContext = mockContextWithServices();
    when(appArtifactContext.getMuleContext()).thenReturn(appMuleContext);

    appRegistry = appArtifactContext.getRegistry();
    doReturn(of(appMuleContext)).when(appRegistry).lookupByType(MuleContext.class);

    doReturn(appRegistry).when(application).getRegistry();

    policyRegistry = mock(MuleRegistry.class);
    when(policyRegistry.lookupObject(FeatureFlaggingService.class)).thenReturn(mock(FeatureFlaggingService.class));

    policyMuleContext = mock(MuleContextWithRegistry.class);
    when(policyMuleContext.getRegistry()).thenReturn(policyRegistry);
    when(policyMuleContext.getInjector()).thenReturn(mock(Injector.class));
  }

  @Test
  @Issue("MULE-14289")
  public void correctArtifactTypeForPolicies() throws Exception {
    MuleContextListener muleContextListener = mock(MuleContextListener.class);
    ArgumentCaptor<MuleContext> muleContextCaptor = ArgumentCaptor.forClass(MuleContext.class);

    doReturn(of(mock(ExtensionManager.class))).when(appRegistry).lookupByName(OBJECT_EXTENSION_MANAGER);

    PolicyParametrization parameterization =
        new PolicyParametrization("policyId", mock(PolicyPointcut.class), 1, emptyMap(), mock(File.class), emptyList());
    DefaultApplicationPolicyInstance applicationPolicyInstance =
        new DefaultApplicationPolicyInstance(application, policyTemplate,
                                             parameterization,
                                             serviceRepository,
                                             mock(ClassLoaderRepository.class),
                                             mock(ExtensionModelLoaderRepository.class),
                                             muleContextListener,
                                             mock(ArtifactConfigurationProcessor.class));

    try {
      applicationPolicyInstance.initialise();
    } catch (InitialisationException e) {
      // Initialization is expected to fail because of missing context
      // But the data to do the assertion on must already have been called
    }

    verify(muleContextListener).onCreation(muleContextCaptor.capture());
    assertThat(muleContextCaptor.getValue().getArtifactType(), is(POLICY));
  }

  @Test
  @Issue("W-11233266")
  public void policyWithHttpOnAppWithHttpUsesHttpFromTheApp() throws Exception {
    // add http and sockets as dependencies in the app...
    ExtensionManager appExtensionManager = mock(ExtensionManager.class);
    ExtensionModel appHttpExtModel = mockExtensionModel("HTTP");
    ExtensionModel appSocketsExtModel = mockExtensionModel("Sockets");
    doReturn(of(appHttpExtModel)).when(appExtensionManager).getExtension("HTTP");
    doReturn(of(appSocketsExtModel)).when(appExtensionManager).getExtension("Sockets");
    doReturn(new HashSet<>(asList(appHttpExtModel, appSocketsExtModel))).when(appExtensionManager)
        .getExtensions();

    doReturn(of(appExtensionManager)).when(appRegistry).lookupByName(OBJECT_EXTENSION_MANAGER);

    // add http and sockets as plugin dependencies declared in the policies...
    doReturn(asList(mockArtifactPlugin("HTTP"), mockArtifactPlugin("Sockets")))
        .when(policyTemplate).getOwnArtifactPlugins();

    ExtensionModelDiscoverer extModelDiscoverer = mock(ExtensionModelDiscoverer.class);
    setExtModelDiscovererFactory(extModelDiscoverer);

    PolicyExtensionManagerFactory policyExtensionManagerFactory =
        new PolicyExtensionManagerFactory(application, policyTemplate, mock(ExtensionModelLoaderRepository.class));

    policyExtensionManagerFactory.create(policyMuleContext);

    ArgumentCaptor<ExtensionDiscoveryRequest> extDiscoveryRequestCaptor =
        ArgumentCaptor.forClass(ExtensionDiscoveryRequest.class);
    verify(extModelDiscoverer).discoverPluginsExtensionModels(extDiscoveryRequestCaptor.capture());

    List<String> policyExtensionNames = extDiscoveryRequestCaptor.getValue().getArtifactPluginDescriptors().stream()
        .map(em -> em.getName())
        .collect(toList());;

    // ... the policy does not have itself http or sockets becuase it uses the ones form the app
    assertThat(policyExtensionNames.toString(), policyExtensionNames, not(hasItems("HTTP", "Sockets")));
  }

  @Test
  @Issue("W-11233266")
  public void policyWithHttpOnAppWithoutHttpUsesHttpFromThePolicy() throws Exception {
    // do NOT add http as dependencies in the app...
    ExtensionManager appExtensionManager = mock(ExtensionManager.class);
    ExtensionModel appSocketsExtModel = mockExtensionModel("Sockets");
    doReturn(of(appSocketsExtModel)).when(appExtensionManager).getExtension("Sockets");
    doReturn(singleton(appSocketsExtModel)).when(appExtensionManager).getExtensions();

    doReturn(of(appExtensionManager)).when(appRegistry).lookupByName(OBJECT_EXTENSION_MANAGER);

    // add http and sockets as plugin dependencies declared in the policies...
    doReturn(asList(mockArtifactPlugin("HTTP"), mockArtifactPlugin("Sockets")))
        .when(policyTemplate).getOwnArtifactPlugins();

    ExtensionModelDiscoverer extModelDiscoverer = mock(ExtensionModelDiscoverer.class);
    setExtModelDiscovererFactory(extModelDiscoverer);

    PolicyExtensionManagerFactory policyExtensionManagerFactory =
        new PolicyExtensionManagerFactory(application, policyTemplate, mock(ExtensionModelLoaderRepository.class));

    policyExtensionManagerFactory.create(policyMuleContext);

    ArgumentCaptor<ExtensionDiscoveryRequest> extDiscoveryRequestCaptor =
        ArgumentCaptor.forClass(ExtensionDiscoveryRequest.class);
    verify(extModelDiscoverer).discoverPluginsExtensionModels(extDiscoveryRequestCaptor.capture());

    List<String> policyExtensionNames = extDiscoveryRequestCaptor.getValue().getArtifactPluginDescriptors().stream()
        .map(em -> em.getName())
        .collect(toList());;

    // ... the policy has itself http but not sockets
    assertThat(policyExtensionNames.toString(), policyExtensionNames, allOf(hasItem("HTTP"), not(hasItem("Sockets"))));
  }

  private ArtifactPlugin mockArtifactPlugin(String name) {
    ArtifactPluginDescriptor artifactPluginDescriptor = mock(ArtifactPluginDescriptor.class);
    when(artifactPluginDescriptor.getName()).thenReturn(name);
    when(artifactPluginDescriptor.getBundleDescriptor()).thenReturn(new BundleDescriptor.Builder()
        .setGroupId("mule")
        .setArtifactId(name)
        .setVersion("1.0.0")
        .build());
    when(artifactPluginDescriptor.getClassLoaderModel()).thenReturn(NULL_CLASSLOADER_MODEL);

    ArtifactPlugin artifactPlugin = mock(ArtifactPlugin.class);
    when(artifactPlugin.getDescriptor()).thenReturn(artifactPluginDescriptor);
    when(artifactPlugin.getArtifactClassLoader()).thenReturn(mock(ArtifactClassLoader.class));

    return artifactPlugin;
  }

  private ExtensionModel mockExtensionModel(String name) {
    ExtensionModel extModel = mock(ExtensionModel.class);
    when(extModel.getName()).thenReturn(name);

    return extModel;
  }

  private void setExtModelDiscovererFactory(ExtensionModelDiscoverer extModelDiscoverer) throws Exception {
    BiFunction<PluginClassLoaderSupplier, ExtensionModelLoaderRepository, ExtensionModelDiscoverer> extModelDiscovererFactory =
        (pcl, eml) -> extModelDiscoverer;

    Field extModelDiscovererField = ArtifactExtensionManagerFactory.class.getDeclaredField("EXT_MODEL_DISCOVERER_FACTORY");
    extModelDiscovererField.setAccessible(true);
    extModelDiscovererField.set(null, extModelDiscovererFactory);
  }
}
