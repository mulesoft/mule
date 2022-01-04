/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.profiling.NoOpProfilingService;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

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

  private org.mule.runtime.core.internal.registry.Registry originalRegistry;

  public BaseSpringMuleContextServiceConfigurator(MuleContext muleContext,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator,
                                                  org.mule.runtime.core.internal.registry.Registry originalRegistry) {
    super((CustomServiceRegistry) muleContext.getCustomizationService(), beanDefinitionRegistry, serviceLocator);
    this.originalRegistry = originalRegistry;
  }

  void createArtifactServices() {
    registerConstantBeanDefinition(FEATURE_FLAGGING_SERVICE_KEY, originalRegistry.lookupObject(FEATURE_FLAGGING_SERVICE_KEY));

    registerConstantBeanDefinition(ConfigurationComponentLocator.REGISTRY_KEY, new BaseConfigurationComponentLocator());

    // Instances of the repository and locator need to be injected into another objects before actually determining the possible
    // values. This contributing layer is needed to ensure the correct functioning of the DI mechanism while allowing actual
    // values to be provided at a later time.
    final ContributedErrorTypeRepository contributedErrorTypeRepository = new ContributedErrorTypeRepository();
    registerConstantBeanDefinition(ErrorTypeRepository.class.getName(), contributedErrorTypeRepository);
    final ContributedErrorTypeLocator contributedErrorTypeLocator = new ContributedErrorTypeLocator();
    contributedErrorTypeLocator.setDelegate(createDefaultErrorTypeLocator(contributedErrorTypeRepository));
    registerConstantBeanDefinition(ErrorTypeLocator.class.getName(), contributedErrorTypeLocator);

    registerBeanDefinition(OBJECT_SCHEDULER_POOLS_CONFIG,
                           getConstantObjectBeanDefinition(SchedulerContainerPoolsConfig.getInstance()));
    registerBeanDefinition(OBJECT_SCHEDULER_BASE_CONFIG, getBeanDefinition(SchedulerBaseConfigFactory.class));

    registerBeanDefinition(MULE_PROFILING_SERVICE_KEY, getBeanDefinition(NoOpProfilingService.class));

    createRuntimeServices();
    absorbOriginalRegistry();
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

  private void absorbOriginalRegistry() {
    if (originalRegistry == null) {
      return;
    }

    originalRegistry.lookupByType(Object.class)
        .forEach((key, value) -> registerConstantBeanDefinition(key, value));
    originalRegistry = null;
  }

}
