/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyTemplateFactory.createPolicyTemplateCreationErrorMessage;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.license.api.LicenseValidatorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultPolicyTemplateFactoryTestCase extends AbstractMuleTestCase {

  private static final String POLICY_ID = "policyId";
  private static final String POLICY_NAME = "testPolicy";

  private final PolicyTemplateClassLoaderBuilderFactory classLoaderBuilderFactory =
      mock(PolicyTemplateClassLoaderBuilderFactory.class);
  private final PluginDependenciesResolver pluginDependenciesResolver =
      mock(PluginDependenciesResolver.class);
  private final PolicyTemplateFactory policyTemplateFactory =
      new DefaultPolicyTemplateFactory(classLoaderBuilderFactory, pluginDependenciesResolver,
                                       discoverLicenseValidator(getClass().getClassLoader()));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private final PolicyTemplateDescriptor descriptor = new PolicyTemplateDescriptor(POLICY_NAME);

  @Test
  public void createsPolicyTemplate() throws Exception {

    RegionClassLoader regionClassLoader = createRegionClassLoader();
    PolicyTemplateClassLoaderBuilder policyTemplateClassLoaderBuilder = createPolicyTemplateClassLoaderBuilder(regionClassLoader);

    MuleDeployableArtifactClassLoader policyClassLoader = mock(MuleDeployableArtifactClassLoader.class);
    when(policyClassLoader.getArtifactId()).thenReturn(POLICY_ID);
    when(policyTemplateClassLoaderBuilder.build()).thenReturn(policyClassLoader);
    when(classLoaderBuilderFactory.createArtifactClassLoaderBuilder()).thenReturn(policyTemplateClassLoaderBuilder);

    PolicyTemplate policyTemplate = policyTemplateFactory.createArtifact(createApplication(regionClassLoader), descriptor);

    assertThat(policyTemplate.getArtifactClassLoader(), is(policyClassLoader));
    assertThat(policyTemplate.getDescriptor(), is(descriptor));
    assertThat(policyTemplate.getArtifactId(), is(POLICY_ID));
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), equalTo(1));
  }

  @Test
  public void managesArtifactContextCreationFailure() throws Exception {
    RegionClassLoader regionClassLoader = createRegionClassLoader();
    PolicyTemplateClassLoaderBuilder policyTemplateClassLoaderBuilder = createPolicyTemplateClassLoaderBuilder(regionClassLoader);

    final String errorMessage = "Error";
    final IOException exceptionCause = new IOException(errorMessage);
    when(policyTemplateClassLoaderBuilder.build()).thenThrow(exceptionCause);
    when(classLoaderBuilderFactory.createArtifactClassLoaderBuilder()).thenReturn(policyTemplateClassLoaderBuilder);

    this.expectedException.expect(PolicyTemplateCreationException.class);
    this.expectedException.expectMessage(createPolicyTemplateCreationErrorMessage(POLICY_NAME));
    this.expectedException.expectCause(equalTo(exceptionCause));
    policyTemplateFactory.createArtifact(createApplication(regionClassLoader), descriptor);

    // Checks that the region was not updated
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), equalTo(0));
  }

  private Application createApplication(RegionClassLoader regionClassLoader) {
    ApplicationDescriptor appDescriptor = mock(ApplicationDescriptor.class);
    when(appDescriptor.getPlugins()).thenReturn(emptySet());
    Application application = mock(Application.class);
    when(application.getRegionClassLoader()).thenReturn(regionClassLoader);
    when(application.getDescriptor()).thenReturn(appDescriptor);

    final Domain domain = mock(Domain.class);
    when(domain.getDescriptor()).thenReturn(new DomainDescriptor("testDomain"));
    when(application.getDomain()).thenReturn(domain);

    return application;
  }

  private PolicyTemplateClassLoaderBuilder createPolicyTemplateClassLoaderBuilder(RegionClassLoader regionClassLoader) {
    PolicyTemplateClassLoaderBuilder policyTemplateClassLoaderBuilder = mock(PolicyTemplateClassLoaderBuilder.class);
    when(policyTemplateClassLoaderBuilder.setParentClassLoader(regionClassLoader)).thenReturn(policyTemplateClassLoaderBuilder);
    when(policyTemplateClassLoaderBuilder.setArtifactDescriptor(descriptor)).thenReturn(policyTemplateClassLoaderBuilder);
    when(policyTemplateClassLoaderBuilder.addArtifactPluginDescriptors(anyVararg())).thenReturn(policyTemplateClassLoaderBuilder);
    return policyTemplateClassLoaderBuilder;
  }

  private RegionClassLoader createRegionClassLoader() {
    ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    RegionClassLoader regionClassLoader =
        new RegionClassLoader(descriptor.getName(), descriptor, this.getClass().getClassLoader(),
                              lookupPolicy);

    // Adds the owner of the region
    ArtifactClassLoader regionOwnerClassLoader = mock(ArtifactClassLoader.class);
    regionClassLoader.addClassLoader(regionOwnerClassLoader, NULL_CLASSLOADER_FILTER);

    return regionClassLoader;
  }
}
