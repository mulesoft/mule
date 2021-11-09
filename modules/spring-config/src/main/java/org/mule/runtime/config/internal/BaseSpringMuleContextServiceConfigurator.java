/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.getInstance;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.config.internal.factories.FixedTypeConstantFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder;
import org.mule.runtime.core.internal.util.TypeSupplier;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;

import java.lang.reflect.InvocationHandler;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
class BaseSpringMuleContextServiceConfigurator {

  private final MuleContext muleContext;
  private final CustomServiceRegistry customServiceRegistry;
  private final BeanDefinitionRegistry beanDefinitionRegistry;

  private final Registry serviceLocator;

  public BaseSpringMuleContextServiceConfigurator(MuleContext muleContext,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator) {
    this.muleContext = muleContext;
    this.customServiceRegistry = (CustomServiceRegistry) muleContext.getCustomizationService();
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.serviceLocator = serviceLocator;
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

  private void registerConstantBeanDefinition(String serviceId, Object impl) {
    registerBeanDefinition(serviceId, getConstantObjectBeanDefinition(impl));
  }

  private void registerBeanDefinition(String serviceId, BeanDefinition beanDefinition) {
    beanDefinition = customServiceRegistry.getOverriddenService(serviceId)
        .map(customService -> getCustomServiceBeanDefinition(customService, serviceId))
        .orElse(beanDefinition);

    beanDefinitionRegistry.registerBeanDefinition(serviceId, beanDefinition);
  }

  private BeanDefinition getCustomServiceBeanDefinition(CustomService customService, String serviceId) {
    BeanDefinition beanDefinition;

    Optional<Class> customServiceClass = customService.getServiceClass();
    Optional<Object> customServiceImpl = customService.getServiceImpl();
    if (customServiceClass.isPresent()) {
      beanDefinition = getBeanDefinitionBuilder(customServiceClass.get()).getBeanDefinition();
    } else if (customServiceImpl.isPresent()) {
      Object servImpl = customServiceImpl.get();
      if (servImpl instanceof Service) {
        if (isProxyClass(servImpl.getClass())) {
          InvocationHandler handler = getInvocationHandler(servImpl);
          if (handler instanceof LazyServiceProxy) {
            servImpl = ((LazyServiceProxy) handler)
                .forApplication(new InjectParamsFromContextServiceMethodInvoker(serviceLocator));
          }

          beanDefinition = servImpl instanceof TypeSupplier
              ? getFixedTypeConstantObjectBeanDefinition(servImpl, (Class<?>) ((TypeSupplier) servImpl).getType())
              : getConstantObjectBeanDefinition(servImpl);

        } else {
          beanDefinition =
              getConstantObjectBeanDefinition(createInjectProviderParamsServiceProxy((Service) servImpl, serviceLocator));
        }
      } else {
        beanDefinition = getConstantObjectBeanDefinition(servImpl);
      }
    } else {
      throw new IllegalStateException("A custom service must define a service class or instance");
    }

    if (OBJECT_STORE_MANAGER.equals(serviceId)) {
      beanDefinition.setPrimary(true);
    }

    return beanDefinition;
  }

  private static BeanDefinition getConstantObjectBeanDefinition(Object impl) {
    return getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgValue(impl).getBeanDefinition();
  }

  private static BeanDefinition getFixedTypeConstantObjectBeanDefinition(Object object, Class<?> type) {
    return getBeanDefinitionBuilder(FixedTypeConstantFactoryBean.class)
        .addConstructorArgValue(object)
        .addConstructorArgValue(type)
        .getBeanDefinition();
  }

  private static BeanDefinitionBuilder getBeanDefinitionBuilder(Class<?> beanType) {
    return genericBeanDefinition(beanType);
  }

}
