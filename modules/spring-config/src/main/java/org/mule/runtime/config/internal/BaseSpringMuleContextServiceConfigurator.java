/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.getInstance;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder;

import javax.inject.Inject;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * This class configured all the services available in a {@code MuleContext} that are independent of the artifact config.
 * <p>
 * There's a predefined set of services plus a configurable set of services provided by
 * {@code MuleContext#getCustomizationService}.
 * <p>
 * This class takes cares of registering bean definitions for each of the provided services so dependency injection can be
 * properly done through the use of {@link Inject}.
 *
 * @since 4.0
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
    // Initial feature flagging service setup
    FeatureFlaggingRegistry ffRegistry = getInstance();
    FeatureFlaggingService featureFlaggingService = new FeatureFlaggingServiceBuilder()
        .withContext(muleContext)
        .withContext(new FeatureContext(muleContext.getConfiguration().getMinMuleVersion().orElse(null), muleContext.getId()))
        .withMuleContextFlags(ffRegistry.getFeatureConfigurations())
        .withFeatureContextFlags(ffRegistry.getFeatureFlagConfigurations())
        .build();
    registerConstantBeanDefinition(FEATURE_FLAGGING_SERVICE_KEY, featureFlaggingService);
  }

}
