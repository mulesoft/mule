/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.api.config.MuleRuntimeFeature.SEPARATE_CLASSLOADER_FOR_POLICY_ISOLATION;
import static org.mule.runtime.module.artifact.internal.util.FeatureFlaggingUtils.isFeatureEnabled;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentSuccessfulStory.POLICY_ISOLATION;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.mule.runtime.container.internal.FilteringContainerClassLoader;
import org.mule.runtime.container.internal.IsolatedPolicyClassLoader;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.artifact.internal.util.FeatureFlaggingUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

@Feature(POLICY_DEPLOYMENT)
@Story(POLICY_ISOLATION)
@Issue("W-17340911")
public class DefaultPolicyTemplateFactoryIsolationTestCase extends AbstractMuleTestCase {

  private static final String POLICY_ID = "policyId";
  private static final String POLICY_NAME = "testPolicy";

  private final PolicyTemplateClassLoaderBuilderFactory classLoaderBuilderFactory =
      mock(PolicyTemplateClassLoaderBuilderFactory.class);
  private final PluginDependenciesResolver pluginDependenciesResolver =
      mock(PluginDependenciesResolver.class);
  private final PolicyTemplateFactory policyTemplateFactory =
      new DefaultPolicyTemplateFactory(classLoaderBuilderFactory, pluginDependenciesResolver,
                                       discoverLicenseValidator(getClass().getClassLoader()));
  private PolicyTemplateDescriptor descriptor;
  private MuleDeployableArtifactClassLoader policyClassLoader;
  private MuleDeployableArtifactClassLoader ownPolicyClassLoader;

  @Before
  public void setup() {
    descriptor = new PolicyTemplateDescriptor(POLICY_NAME);
    descriptor.setPlugins(createHttpPluginDescriptor());
    policyClassLoader = mock(MuleDeployableArtifactClassLoader.class);
    ownPolicyClassLoader = mock(MuleDeployableArtifactClassLoader.class);
    when(policyClassLoader.getArtifactId()).thenReturn(POLICY_ID);
    when(ownPolicyClassLoader.getArtifactId()).thenReturn(POLICY_ID);
    when(policyClassLoader.getArtifactPluginClassLoaders()).thenReturn(emptyList());
    when(ownPolicyClassLoader.getArtifactPluginClassLoaders()).thenReturn(emptyList());
    when(pluginDependenciesResolver.resolve(any(), any(), eq(false))).thenReturn(emptyList());
  }

  @Test
  public void testPolicyIsolationEnabled() {
    testParentClassLoader(true, true, true);
  }

  @Test
  public void testPolicyIsolationDisabled() {
    testParentClassLoader(false, false, true);
  }

  @Test
  public void testPolicyIsolationEnabledNoRequiredPlugin() {
    descriptor.setPlugins(Collections.emptySet());
    testParentClassLoader(true, true, false);
  }

  private void testParentClassLoader(boolean enableIsolation, boolean useSeparateClassloader, boolean hasRequiredPlugin) {
    try (MockedStatic<FeatureFlaggingUtils> mockedFeatureFlaggingUtils = mockStatic(FeatureFlaggingUtils.class)) {
      mockedFeatureFlaggingUtils.when(() -> isFeatureEnabled(ENABLE_POLICY_ISOLATION, descriptor))
          .thenReturn(enableIsolation);
      mockedFeatureFlaggingUtils
          .when(() -> isFeatureEnabled(SEPARATE_CLASSLOADER_FOR_POLICY_ISOLATION, descriptor))
          .thenReturn(useSeparateClassloader);

      // mock application and regionClassLoader
      RegionClassLoader appRegionClassLoader = createRegionClassLoader();
      Application application = createApplication(appRegionClassLoader);

      // mock filteringContainerClassLoader
      FilteringContainerClassLoader containerClassLoader = mock(FilteringContainerClassLoader.class);
      when(classLoaderBuilderFactory.getFilteringContainerClassLoader()).thenReturn(containerClassLoader);
      when(containerClassLoader.getArtifactId()).thenReturn("containerArtifactId");
      ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
      when(containerClassLoader.getClassLoaderLookupPolicy()).thenReturn(lookupPolicy);
      ArtifactDescriptor containerArtifactDescriptor = mock(ArtifactDescriptor.class);
      when(containerClassLoader.getArtifactDescriptor()).thenReturn(containerArtifactDescriptor);

      // mock policyTemplateClassLoaderBuilder
      PolicyTemplateClassLoaderBuilder policyTemplateClassLoaderBuilder = createPolicyTemplateClassLoaderBuilder();

      // set up parent classloaders
      if (enableIsolation && useSeparateClassloader && hasRequiredPlugin) {
        when(policyTemplateClassLoaderBuilder.setParentClassLoader(any(IsolatedPolicyClassLoader.class)))
            .thenAnswer(invocation -> {
              ClassLoader parent = invocation.getArgument(0);
              when(policyClassLoader.getParent()).thenReturn(parent);
              return policyTemplateClassLoaderBuilder;
            });
        when(policyTemplateClassLoaderBuilder.build()).thenReturn(policyClassLoader, ownPolicyClassLoader);
      } else {
        when(policyTemplateClassLoaderBuilder.setParentClassLoader(appRegionClassLoader))
            .thenAnswer(invocation -> {
              ClassLoader parent = invocation.getArgument(0);
              when(policyClassLoader.getParent()).thenReturn(parent);
              return policyTemplateClassLoaderBuilder;
            });
        when(policyTemplateClassLoaderBuilder.build()).thenReturn(policyClassLoader, ownPolicyClassLoader);
      }
      when(classLoaderBuilderFactory.createArtifactClassLoaderBuilder()).thenReturn(policyTemplateClassLoaderBuilder);

      policyTemplateFactory.createArtifact(application, descriptor);
      ClassLoader actualParentClassLoader = policyClassLoader.getParent();

      if (enableIsolation && useSeparateClassloader && hasRequiredPlugin) {
        assertThat("Parent class loader should be IsolatedPolicyClassLoader", actualParentClassLoader,
                   instanceOf(IsolatedPolicyClassLoader.class));
      } else {
        assertThat("Parent class loader should be the application's RegionClassLoader", actualParentClassLoader,
                   sameInstance(appRegionClassLoader));
      }
    }
  }

  private Application createApplication(RegionClassLoader regionClassLoader) {
    ApplicationDescriptor appDescriptor = mock(ApplicationDescriptor.class);
    when(appDescriptor.getPlugins()).thenReturn(emptySet());
    Application application = mock(Application.class);
    when(application.getRegionClassLoader()).thenReturn(regionClassLoader);
    when(application.getDescriptor()).thenReturn(appDescriptor);

    final Domain domain = mock(Domain.class);
    when(domain.getDescriptor()).thenReturn(new DomainDescriptor("testDomain"));
    when(domain.getArtifactPlugins()).thenReturn(emptyList());
    when(application.getDomain()).thenReturn(domain);
    when(application.getArtifactPlugins()).thenReturn(emptyList());

    return application;
  }

  private PolicyTemplateClassLoaderBuilder createPolicyTemplateClassLoaderBuilder() {
    PolicyTemplateClassLoaderBuilder policyTemplateClassLoaderBuilder = mock(PolicyTemplateClassLoaderBuilder.class);
    when(policyTemplateClassLoaderBuilder.setArtifactDescriptor(any())).thenReturn(policyTemplateClassLoaderBuilder);
    when(policyTemplateClassLoaderBuilder.addArtifactPluginDescriptors(any())).thenReturn(policyTemplateClassLoaderBuilder);
    return policyTemplateClassLoaderBuilder;
  }

  private RegionClassLoader createRegionClassLoader() {
    ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    RegionClassLoader regionClassLoader =
        new RegionClassLoader(descriptor.getName(), descriptor, this.getClass().getClassLoader(),
                              lookupPolicy);

    return regionClassLoader;
  }

  private Set<ArtifactPluginDescriptor> createHttpPluginDescriptor() {
    HashSet<ArtifactPluginDescriptor> plugins = new HashSet<>();
    ArtifactPluginDescriptor httpPlugin = mock(ArtifactPluginDescriptor.class);
    when(httpPlugin.getName()).thenReturn("HTTP");
    plugins.add(httpPlugin);
    return plugins;
  }
}
