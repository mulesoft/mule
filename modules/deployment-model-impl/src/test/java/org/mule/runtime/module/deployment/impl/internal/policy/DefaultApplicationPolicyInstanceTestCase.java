/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.qameta.allure.Issue;

public class DefaultApplicationPolicyInstanceTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-14289")
  @Ignore("MULE-14289: The discovered ArtifactConfigurationProcessor is not compatible with the provided mocks.")
  public void correctArtifactTypeForPolicies() throws InitialisationException {
    MuleContextListener muleContextListener = mock(MuleContextListener.class);
    ArgumentCaptor<MuleContext> muleContextCaptor = ArgumentCaptor.forClass(MuleContext.class);

    PolicyTemplate policyTemplate = mock(PolicyTemplate.class, RETURNS_DEEP_STUBS);
    when(policyTemplate.getArtifactClassLoader().getClassLoader()).thenReturn(this.getClass().getClassLoader());

    Application application = mock(Application.class, RETURNS_DEEP_STUBS);

    Registry registry = application.getRegistry();
    doReturn(of(mockContextWithServices())).when(registry).lookupByType(MuleContext.class);
    doReturn(of(mock(ExtensionManager.class))).when(registry).lookupByName(OBJECT_EXTENSION_MANAGER);

    PolicyParametrization parameterization = mock(PolicyParametrization.class, RETURNS_DEEP_STUBS);
    when(parameterization.getId()).thenReturn("policyId");

    DefaultApplicationPolicyInstance applicationPolicyInstance =
        new DefaultApplicationPolicyInstance(application, policyTemplate,
                                             parameterization, mock(ServiceRepository.class),
                                             mock(ClassLoaderRepository.class),
                                             emptyList(),
                                             mock(ExtensionModelLoaderRepository.class),
                                             muleContextListener);

    applicationPolicyInstance.initialise();

    verify(muleContextListener).onCreation(muleContextCaptor.capture());

    assertThat(muleContextCaptor.getValue().getArtifactType(), is(POLICY));
  }
}
