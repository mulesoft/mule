/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.config.custom.ServiceConfigurator.lookupServiceConfigurators;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXCEPTION_LOCATION_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceMethodInvoker;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Base class for populating constant bean definitions into a {@link Registry}.
 * 
 * @since 4.5
 */
abstract class AbstractSpringMuleContextServiceConfigurator {

  private static final Set<String> APPLICATION_ONLY_SERVICES = Set.of(OBJECT_SECURITY_MANAGER,
                                                                      OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER,
                                                                      OBJECT_MULE_STREAM_CLOSER_SERVICE,
                                                                      OBJECT_PROCESSING_TIME_WATCHER,
                                                                      OBJECT_POLICY_MANAGER,
                                                                      OBJECT_EXCEPTION_LOCATION_PROVIDER,
                                                                      OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER);

  private final InternalCustomizationService customizationService;
  private final BeanDefinitionRegistry beanDefinitionRegistry;
  private final Registry serviceLocator;

  protected AbstractSpringMuleContextServiceConfigurator(InternalCustomizationService customizationService,
                                                         BeanDefinitionRegistry beanDefinitionRegistry,
                                                         Registry serviceLocator) {
    this.customizationService = customizationService;
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.serviceLocator = serviceLocator;
  }

  protected void registerContextServices(Map<String, BeanDefinition> contextServices) {
    contextServices.entrySet().stream()
        .filter(service -> !APPLICATION_ONLY_SERVICES.contains(service.getKey()))
        .forEach(service -> registerBeanDefinition(service.getKey(), service.getValue()));
  }

  protected static BeanDefinition getBeanDefinition(Class<?> beanType) {
    return getBeanDefinitionBuilder(beanType).getBeanDefinition();
  }

  protected void registerConstantBeanDefinition(String serviceId, Object impl) {
    registerConstantBeanDefinition(serviceId, impl, false);
  }

  protected void registerConstantBeanDefinition(String serviceId, Object impl, boolean inject) {
    registerBeanDefinition(serviceId, getConstantObjectBeanDefinition(impl, inject));
  }

  protected void registerBeanDefinition(String serviceId, BeanDefinition beanDefinition) {
    beanDefinition = customizationService.getOverriddenService(serviceId)
        .map(customService -> getCustomServiceBeanDefinition(customService, serviceId))
        .orElse(beanDefinition);

    beanDefinitionRegistry.registerBeanDefinition(serviceId, beanDefinition);
  }

  protected void loadServiceConfigurators() {
    lookupServiceConfigurators(this.getClass().getClassLoader())
        .forEach(customizationInfo -> customizationInfo.configure(getCustomizationService()));
  }

  protected boolean isServiceRuntimeProvided(final CustomService<?> customService) {
    return customService.getServiceImpl().map(Service.class::isInstance).orElse(false)
        || customService.getServiceClass().map(Service.class::isAssignableFrom).orElse(false);
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

          beanDefinition = getConstantObjectBeanDefinition(servImpl, true);
        } else {
          beanDefinition =
              getConstantObjectBeanDefinition(createInjectProviderParamsServiceProxy((Service) servImpl, serviceLocator), true);
        }
      } else {
        beanDefinition = getConstantObjectBeanDefinition(servImpl, true);
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
    return getConstantObjectBeanDefinition(impl, false);
  }

  protected static BeanDefinition getConstantObjectBeanDefinition(Object impl, boolean inject) {
    return getBeanDefinitionBuilder(ConstantFactoryBean.class)
        .addConstructorArgValue(impl)
        .addConstructorArgValue(inject)
        .getBeanDefinition();
  }

  protected static BeanDefinitionBuilder getBeanDefinitionBuilder(Class<?> beanType) {
    return genericBeanDefinition(beanType);
  }

  protected boolean containsBeanDefinition(String beanName) {
    return beanDefinitionRegistry.containsBeanDefinition(beanName);
  }

  protected InternalCustomizationService getCustomizationService() {
    return customizationService;
  }

  protected BeanDefinitionRegistry getBeanDefinitionRegistry() {
    return beanDefinitionRegistry;
  }

  protected Registry getServiceLocator() {
    return serviceLocator;
  }
}
