/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.config.internal.factories.FixedTypeConstantFactoryBean;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.util.TypeSupplier;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;

import java.lang.reflect.InvocationHandler;
import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Base class for populating constant bean definitions into a {@link Registry}.
 * 
 * @since 4.5
 */
abstract class AbstractSpringMuleContextServiceConfigurator {

  private final CustomServiceRegistry customServiceRegistry;
  private final BeanDefinitionRegistry beanDefinitionRegistry;
  private final Registry serviceLocator;

  protected AbstractSpringMuleContextServiceConfigurator(CustomServiceRegistry customServiceRegistry,
                                                         BeanDefinitionRegistry beanDefinitionRegistry,
                                                         Registry serviceLocator) {
    this.customServiceRegistry = customServiceRegistry;
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.serviceLocator = serviceLocator;
  }

  protected static BeanDefinition getBeanDefinition(Class<?> beanType) {
    return getBeanDefinitionBuilder(beanType).getBeanDefinition();
  }

  protected void registerConstantBeanDefinition(String serviceId, Object impl) {
    registerBeanDefinition(serviceId, getConstantObjectBeanDefinition(impl));
  }

  protected void registerBeanDefinition(String serviceId, BeanDefinition beanDefinition) {
    beanDefinition = customServiceRegistry.getOverriddenService(serviceId)
        .map(customService -> getCustomServiceBeanDefinition(customService, serviceId))
        .orElse(beanDefinition);

    beanDefinitionRegistry.registerBeanDefinition(serviceId, beanDefinition);
  }

  protected boolean isServiceRuntimeProvided(final CustomService customService) {
    return customService.getServiceImpl().map(impl -> impl instanceof Service).orElse(false)
        || customService.getServiceClass().map(cls -> Service.class.isAssignableFrom(cls)).orElse(false);
  }

  /**
   * Determine the best way to create a bean definition from a custom service, depending on:
   * <ul>
   * <li>Whether the class or an implementation of a service has been configured.</li>
   * <li>Whether the service is actually custom or is one provided by the runtime.</li>
   * <li>In the case of services provided by the runtime, make those be initialized lazily.</li>
   * </ul>
   */
  // TODO MULE-19927 refactor this code for simplicity
  protected BeanDefinition getCustomServiceBeanDefinition(CustomService customService, String serviceId) {
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

  protected static BeanDefinition getConstantObjectBeanDefinition(Object impl) {
    return getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgValue(impl).getBeanDefinition();
  }

  protected static BeanDefinition getFixedTypeConstantObjectBeanDefinition(Object object, Class<?> type) {
    return getBeanDefinitionBuilder(FixedTypeConstantFactoryBean.class)
        .addConstructorArgValue(object)
        .addConstructorArgValue(type)
        .getBeanDefinition();
  }

  protected static BeanDefinitionBuilder getBeanDefinitionBuilder(Class<?> beanType) {
    return genericBeanDefinition(beanType);
  }

  protected boolean containsBeanDefinition(String beanName) {
    return beanDefinitionRegistry.containsBeanDefinition(beanName);
  }

  protected CustomServiceRegistry getCustomServiceRegistry() {
    return customServiceRegistry;
  }

  protected Registry getServiceLocator() {
    return serviceLocator;
  }
}
