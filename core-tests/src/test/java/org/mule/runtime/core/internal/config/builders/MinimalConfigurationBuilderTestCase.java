/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.builders;

import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.custom.CustomizationService.ServiceInterceptor;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapService;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@SmallTest
@Feature(REGISTRY)
@Story(OBJECT_REGISTRATION)
public class MinimalConfigurationBuilderTestCase extends AbstractMuleTestCase {

  private static final String SERVICE_ID = "serviceId";

  private MuleContextWithRegistry muleContext;
  private MuleRegistry registry;
  private BootstrapServiceDiscoverer bootstrapServiceDiscoverer;
  private InternalCustomizationService customizationService;

  @Before
  public void setUp() {
    registry = mock(MuleRegistry.class);
    bootstrapServiceDiscoverer = mock(BootstrapServiceDiscoverer.class);
    customizationService = mock(InternalCustomizationService.class);

    muleContext = mock(MuleContextWithRegistry.class);
    when(muleContext.getRegistry()).thenReturn(registry);
    when(muleContext.getRegistryBootstrapServiceDiscoverer()).thenReturn(bootstrapServiceDiscoverer);
    when(muleContext.getCustomizationService()).thenReturn(customizationService);
    when(muleContext.getExecutionClassLoader()).thenReturn(this.getClass().getClassLoader());

    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(muleContext.getInjector()).thenReturn(mock(Injector.class));
    when(muleContext.getLifecycleManager()).thenReturn(new DefaultLifecycleManager<>("muleContext", muleContext));
  }

  @Test
  @Issue("W-17146121")
  public void override() throws Exception {
    final Properties properties = new Properties();
    final Object overridenService = new ServiceObjectB();

    properties.put(SERVICE_ID, ServiceObjectA.class.getName());
    when(bootstrapServiceDiscoverer.discover())
        .thenReturn(singletonList(new PropertiesBootstrapService(this.getClass().getClassLoader(),
                                                                 properties)));
    when(customizationService.getOverriddenService(SERVICE_ID))
        .thenReturn(of(new CustomService<>(SERVICE_ID, si -> si.overrideServiceImpl(overridenService))));

    final MinimalConfigurationBuilder minimalConfigurationBuilder = new MinimalConfigurationBuilder();

    minimalConfigurationBuilder.doConfigure(muleContext);

    verify(registry, never()).registerObject(eq(SERVICE_ID), any(ServiceObjectA.class));
    verify(registry).registerObject(eq(SERVICE_ID), same(overridenService));
  }

  @Test
  @Issue("W-17146121")
  public void remove() throws Exception {
    final Properties properties = new Properties();

    properties.put(SERVICE_ID, ServiceObjectA.class.getName());
    when(bootstrapServiceDiscoverer.discover())
        .thenReturn(singletonList(new PropertiesBootstrapService(this.getClass().getClassLoader(),
                                                                 properties)));
    when(customizationService.getOverriddenService(SERVICE_ID))
        .thenReturn(of(new CustomService<>(SERVICE_ID, ServiceInterceptor::remove)));

    final MinimalConfigurationBuilder minimalConfigurationBuilder = new MinimalConfigurationBuilder();

    minimalConfigurationBuilder.doConfigure(muleContext);

    verify(registry, never()).registerObject(eq(SERVICE_ID), any(ServiceObjectA.class));
  }

  public static class ServiceObjectA {

  }

  public static class ServiceObjectB {

  }
}
