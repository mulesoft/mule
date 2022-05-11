/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.policy.api.PolicyPointcut;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

import org.junit.Test;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Issue;

public class DefaultApplicationPolicyInstanceTestCase extends AbstractMuleTestCase {

  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);

  @Test
  @Issue("MULE-14289")
  public void correctArtifactTypeForPolicies() throws Exception {
    MuleContextListener muleContextListener = mock(MuleContextListener.class);
    ArgumentCaptor<MuleContext> muleContextCaptor = ArgumentCaptor.forClass(MuleContext.class);

    PolicyTemplate policyTemplate = mock(PolicyTemplate.class, RETURNS_DEEP_STUBS);
    when(policyTemplate.getArtifactClassLoader().getClassLoader()).thenReturn(this.getClass().getClassLoader());
    when(policyTemplate.getDescriptor()).thenReturn(new PolicyTemplateDescriptor("policyId"));

    Application application = mock(Application.class, RETURNS_DEEP_STUBS);
    ArtifactContext appArtifactContext = application.getArtifactContext();
    MuleContextWithRegistry appMuleContext = mockContextWithServices();
    when(appArtifactContext.getMuleContext()).thenReturn(appMuleContext);

    Registry registry = appArtifactContext.getRegistry();
    doReturn(of(appMuleContext)).when(registry).lookupByType(MuleContext.class);
    doReturn(of(mock(ExtensionManager.class))).when(registry).lookupByName(OBJECT_EXTENSION_MANAGER);

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
}
