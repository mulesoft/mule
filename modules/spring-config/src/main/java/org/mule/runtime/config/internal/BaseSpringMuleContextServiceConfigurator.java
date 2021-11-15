/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * This class configures the basic services available in a {@code MuleContext} that are independent of the artifact config.
 * <p>
 * There's a predefined set of services plus a configurable set of services provided by
 * {@code MuleContext#getCustomizationService}.
 * <p>
 * This class takes cares of registering bean definitions for each of the provided services so dependency injection can be
 * properly done through the use of {@link Inject}.
 *
 * @since 4.5
 */
class BaseSpringMuleContextServiceConfigurator extends AbstractSpringMuleContextServiceConfigurator {

  private final MuleContext muleContext;

  public BaseSpringMuleContextServiceConfigurator(MuleContext muleContext,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator) {
    super((CustomServiceRegistry) muleContext.getCustomizationService(), beanDefinitionRegistry, serviceLocator);
    this.muleContext = muleContext;
  }

  void createArtifactServices() {
    registerConstantBeanDefinition(FEATURE_FLAGGING_SERVICE_KEY, ((MuleContextWithRegistry) muleContext).getRegistry()
        .lookupObject(FEATURE_FLAGGING_SERVICE_KEY));

    createRuntimeServices();
  }

  private void createRuntimeServices() {
    final Map<String, CustomService> customServices = getCustomServiceRegistry().getCustomServices();
    for (String serviceName : customServices.keySet()) {

      if (containsBeanDefinition(serviceName)) {
        throw new IllegalStateException("There is already a bean definition registered with key: " + serviceName);
      }

      final CustomService customService = customServices.get(serviceName);
      // TODO MULE-19927 get these form a more specific place and avoid this filter
      if (isServiceRuntimeProvided(customService)) {
        final BeanDefinition beanDefinition = getCustomServiceBeanDefinition(customService, serviceName);

        registerBeanDefinition(serviceName, beanDefinition);
      }
    }
  }

}
