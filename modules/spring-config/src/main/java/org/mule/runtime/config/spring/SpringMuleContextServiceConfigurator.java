/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.config.spring.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_LOCAL_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_COMPONENT_INITIAL_STATE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXCEPTION_LOCATION_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_IN_MEMORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_PERSISTENT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_OBJECT_NAME_PROCESSOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER_STATE_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSOR_INTERCEPTOR_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SERIALIZER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMATION_SERVICE;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_ARTIFACT_PROPERTIES_RESOURCE;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.api.artifact.ArtifactProperties;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.config.spring.factories.ConstantFactoryBean;
import org.mule.runtime.config.spring.factories.ExtensionManagerFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionManagerFactoryBean;
import org.mule.runtime.config.spring.processors.MuleObjectNameProcessor;
import org.mule.runtime.config.spring.processors.ParentContextPropertyPlaceholderProcessor;
import org.mule.runtime.config.spring.processors.PropertyPlaceholderProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.time.TimeSupplier;
import org.mule.runtime.core.component.state.DefaultComponentInitialStateManager;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.factories.HostNameFactory;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MessageProcessingFlowTraceManager;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.RegistryNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.el.DefaultExpressionManager;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.exception.MessagingExceptionLocationProvider;
import org.mule.runtime.core.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.connector.MuleConnectorOperationLocator;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.streaming.DefaultStreamingManager;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.policy.DefaultPolicyManager;
import org.mule.runtime.core.policy.DefaultPolicyStateHandler;
import org.mule.runtime.core.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;
import org.mule.runtime.core.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.core.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.util.DefaultStreamCloserService;
import org.mule.runtime.core.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.util.store.DefaultObjectStoreFactoryBean;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * This class configured all the services available in a {@code MuleContext}.
 * <p>
 * There's a predefined set of services plus a configurable set of services provided by
 * {@code MuleContext#getCustomizationService}.
 * <p>
 * This class takes cares of registering bean definitions for each of the provided services so dependency injection can be
 * properly done through the use of {@link Inject}.
 *
 * @since 4.0
 */
class SpringMuleContextServiceConfigurator {

  private final MuleContext muleContext;
  private final ArtifactType artifactType;
  private final OptionalObjectsController optionalObjectsController;
  private final CustomServiceRegistry customServiceRegistry;
  private final BeanDefinitionRegistry beanDefinitionRegistry;

  private static final ImmutableSet<String> APPLICATION_ONLY_SERVICES =
      ImmutableSet.<String>builder().add(OBJECT_SECURITY_MANAGER).add(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER)
          .add(OBJECT_MULE_STREAM_CLOSER_SERVICE).add(OBJECT_CONVERTER_RESOLVER).add(OBJECT_LOCK_FACTORY)
          .add(OBJECT_LOCK_PROVIDER).add(OBJECT_PROCESSING_TIME_WATCHER).add(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR)
          .add(OBJECT_EXCEPTION_LOCATION_PROVIDER).add(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER).build();

  private static final ImmutableMap<String, String> OBJECT_STORE_NAME_TO_LOCAL_OBJECT_STORE_NAME =
      ImmutableMap.<String, String>builder().put(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, OBJECT_LOCAL_STORE_IN_MEMORY)
          .put(OBJECT_STORE_DEFAULT_PERSISTENT_NAME, OBJECT_LOCAL_STORE_PERSISTENT)
          .put(DEFAULT_USER_OBJECT_STORE_NAME, DEFAULT_LOCAL_USER_OBJECT_STORE_NAME)
          .put(DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME, DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME).build();

  // Do not use static field. BeanDefinitions are reused and produce weird behaviour
  private final ImmutableMap<String, BeanDefinition> defaultContextServices = ImmutableMap.<String, BeanDefinition>builder()
      .put(OBJECT_TRANSACTION_MANAGER, getBeanDefinition(TransactionManagerFactoryBean.class))
      .put(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, getBeanDefinition(NoRetryPolicyTemplate.class))
      .put(OBJECT_EXPRESSION_LANGUAGE, getBeanDefinition(MVELExpressionLanguage.class))
      .put(OBJECT_EXPRESSION_MANAGER, getBeanDefinition(DefaultExpressionManager.class))
      .put(OBJECT_EXTENSION_MANAGER, getBeanDefinition(ExtensionManagerFactoryBean.class))
      .put(OBJECT_TIME_SUPPLIER, getBeanDefinition(TimeSupplier.class))
      .put(OBJECT_CONNECTION_MANAGER, getBeanDefinition(DefaultConnectionManager.class))
      .put(OBJECT_METADATA_SERVICE, getBeanDefinition(MuleMetadataService.class))
      .put(OBJECT_OBJECT_NAME_PROCESSOR, getBeanDefinition(MuleObjectNameProcessor.class))
      .put(OBJECT_POLICY_MANAGER, getBeanDefinition(DefaultPolicyManager.class))
      .put(OBJECT_PROCESSOR_INTERCEPTOR_MANAGER, getBeanDefinition(DefaultProcessorInterceptorManager.class))
      .put(OBJECT_POLICY_MANAGER_STATE_HANDLER, getBeanDefinition(DefaultPolicyStateHandler.class))
      .put(OBJECT_SERIALIZER,
           getBeanDefinitionBuilder(DefaultObjectSerializerFactoryBean.class).addDependsOn(OBJECT_MULE_CONFIGURATION)
               .getBeanDefinition())
      .put(OBJECT_NOTIFICATION_MANAGER, createNotificationManagerBeanDefinition())
      .put(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_IN_MEMORY)
               .getBeanDefinition())
      .put(OBJECT_LOCAL_STORE_IN_MEMORY,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultInMemoryObjectStore"))
      .put(OBJECT_STORE_DEFAULT_PERSISTENT_NAME,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_PERSISTENT)
               .getBeanDefinition())
      .put(OBJECT_LOCAL_STORE_PERSISTENT,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultPersistentObjectStore"))
      .put(DEFAULT_USER_OBJECT_STORE_NAME,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(DEFAULT_LOCAL_USER_OBJECT_STORE_NAME)
               .getBeanDefinition())
      .put(DEFAULT_LOCAL_USER_OBJECT_STORE_NAME,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultUserObjectStore"))
      .put(DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME,
           getBeanDefinitionBuilder(ConstantFactoryBean.class)
               .addConstructorArgReference(DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME).getBeanDefinition())
      .put(DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultUserTransientObjectStore"))
      .put(OBJECT_STORE_MANAGER, getBeanDefinition(MuleObjectStoreManager.class))
      .put(OBJECT_QUEUE_MANAGER,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_QUEUE_MANAGER)
               .getBeanDefinition())
      .put(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class))
      .put("_muleParentContextPropertyPlaceholderProcessor", getBeanDefinition(ParentContextPropertyPlaceholderProcessor.class))
      .put(OBJECT_SECURITY_MANAGER, getBeanDefinition(DefaultMuleSecurityManager.class))
      .put(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, getBeanDefinition(MuleMessageProcessingManager.class))
      .put(OBJECT_MULE_STREAM_CLOSER_SERVICE, getBeanDefinition(DefaultStreamCloserService.class))
      .put(OBJECT_CONVERTER_RESOLVER, getBeanDefinition(DynamicDataTypeConversionResolver.class))
      .put(OBJECT_LOCK_FACTORY, getBeanDefinition(MuleLockFactory.class))
      .put(OBJECT_LOCK_PROVIDER, getBeanDefinition(SingleServerLockProvider.class))
      .put(OBJECT_PROCESSING_TIME_WATCHER, getBeanDefinition(DefaultProcessingTimeWatcher.class))
      .put(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR, getBeanDefinition(MuleConnectorOperationLocator.class))
      .put(OBJECT_EXCEPTION_LOCATION_PROVIDER, getBeanDefinition(MessagingExceptionLocationProvider.class))
      .put(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER, getBeanDefinition(MessageProcessingFlowTraceManager.class))
      .put(OBJECT_CONNECTIVITY_TESTING_SERVICE, getBeanDefinition(DefaultConnectivityTestingService.class))
      .put(OBJECT_COMPONENT_INITIAL_STATE_MANAGER, getBeanDefinition(DefaultComponentInitialStateManager.class))
      .put(OBJECT_STREAMING_MANAGER, getBeanDefinition(DefaultStreamingManager.class))
      .put(OBJECT_TRANSFORMATION_SERVICE, getBeanDefinition(TransformationService.class))
      .put(OBJECT_SCHEDULER_POOLS_CONFIG, getConstantObjectBeanDefinition(SchedulerContainerPoolsConfig.getInstance()))
      .put(OBJECT_SCHEDULER_BASE_CONFIG, getBeanDefinition(SchedulerBaseConfigFactory.class))
      .build();

  private final SpringConfigurationComponentLocator componentLocator;
  private final ArtifactProperties artifactProperties;

  public SpringMuleContextServiceConfigurator(MuleContext muleContext,
                                              ArtifactProperties artifactProperties,
                                              ArtifactType artifactType,
                                              OptionalObjectsController optionalObjectsController,
                                              BeanDefinitionRegistry beanDefinitionRegistry,
                                              SpringConfigurationComponentLocator componentLocator) {
    this.muleContext = muleContext;
    this.artifactProperties = artifactProperties;
    this.customServiceRegistry = (CustomServiceRegistry) muleContext.getCustomizationService();
    this.artifactType = artifactType;
    this.optionalObjectsController = optionalObjectsController;
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.componentLocator = componentLocator;
  }

  void createArtifactServices() {
    registerPropertyPlaceholder();
    registerBeanDefinition(DEFAULT_ARTIFACT_PROPERTIES_RESOURCE, getConstantObjectBeanDefinition(artifactProperties));
    registerBeanDefinition(OBJECT_CONFIGURATION_COMPONENT_LOCATOR, getConstantObjectBeanDefinition(componentLocator));
    loadServiceConfigurators();

    defaultContextServices.entrySet().stream()
        .filter(service -> !APPLICATION_ONLY_SERVICES.contains(service.getKey()) || artifactType.equals(APP))
        .forEach(service -> registerBeanDefinition(service.getKey(), service.getValue()));

    createBootstrapBeanDefinitions();
    createLocalObjectStoreBeanDefinitions();
    createQueueManagerBeanDefinitions();
    createCustomServices();
  }

  private void registerPropertyPlaceholder() {
    registerBeanDefinition("_mulePropertyPlaceholderProcessor", createMulePropertyPlaceholderBeanDefinition());
  }

  private void loadServiceConfigurators() {
    new SpiServiceRegistry()
        .lookupProviders(ServiceConfigurator.class, Service.class.getClassLoader())
        .forEach(customizer -> customizer.configure(customServiceRegistry));
  }

  private void createCustomServices() {
    final Map<String, CustomService> customServices = customServiceRegistry.getCustomServices();
    for (String serviceName : customServices.keySet()) {

      if (beanDefinitionRegistry.containsBeanDefinition(serviceName)) {
        throw new IllegalStateException("There is already a bean definition registered with key: " + serviceName);
      }

      final CustomService customService = customServices.get(serviceName);
      final BeanDefinition beanDefinition = getCustomServiceBeanDefinition(customService);

      registerBeanDefinition(serviceName, beanDefinition);
    }
  }

  private void registerBeanDefinition(String serviceId, BeanDefinition beanDefinition) {
    beanDefinition = customServiceRegistry.getOverriddenService(serviceId)
        .map(this::getCustomServiceBeanDefinition)
        .orElse(beanDefinition);

    beanDefinitionRegistry.registerBeanDefinition(serviceId, beanDefinition);
  }

  private BeanDefinition getCustomServiceBeanDefinition(CustomService customService) {
    BeanDefinition beanDefinition;

    Optional<Class> customServiceClass = customService.getServiceClass();
    Optional<Object> customServiceImpl = customService.getServiceImpl();
    if (customServiceClass.isPresent()) {
      beanDefinition = getBeanDefinitionBuilder(customServiceClass.get()).getBeanDefinition();
    } else if (customServiceImpl.isPresent()) {
      if (customServiceImpl.get() instanceof Service) {
        beanDefinition = getConstantObjectBeanDefinition(createInjectProviderParamsServiceProxy((Service) customServiceImpl.get(),
                                                                                                muleContext));
      } else {
        beanDefinition = getConstantObjectBeanDefinition(customServiceImpl.get());
      }
    } else {
      throw new IllegalStateException("A custom service must define a service class or instance");
    }
    return beanDefinition;
  }

  private BeanDefinition createMulePropertyPlaceholderBeanDefinition() {
    HashMap<Object, Object> factories = new HashMap<>();
    factories.put("hostname", new HostNameFactory());
    Properties properties = new Properties();
    properties.putAll(artifactProperties.toImmutableMap());
    BeanDefinitionBuilder mulePropertyPlaceholderProcessor = getBeanDefinitionBuilder(PropertyPlaceholderProcessor.class);
    return mulePropertyPlaceholderProcessor.addPropertyValue("properties", properties).addPropertyValue("factories", factories)
        .addPropertyValue("ignoreUnresolvablePlaceholders", true).getBeanDefinition();
  }

  private void createQueueManagerBeanDefinitions() {
    if (customServiceRegistry.getOverriddenService(OBJECT_QUEUE_MANAGER).isPresent()) {
      registerBeanDefinition(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinitionBuilder(ConstantFactoryBean.class)
          .addConstructorArgReference(OBJECT_LOCAL_QUEUE_MANAGER).getBeanDefinition());
    } else {
      registerBeanDefinition(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class));
    }
  }

  private void createLocalObjectStoreBeanDefinitions() {
    AtomicBoolean anyBaseStoreWasRedefined = new AtomicBoolean(false);
    OBJECT_STORE_NAME_TO_LOCAL_OBJECT_STORE_NAME.entrySet().forEach(objectStoreLocal -> {
      customServiceRegistry.getOverriddenService(objectStoreLocal.getKey()).ifPresent(customService -> {
        customService.getServiceClass().ifPresent(serviceClass -> {
          anyBaseStoreWasRedefined.set(true);
          final BeanDefinition defaultBeanDefinition = defaultContextServices.get(objectStoreLocal.getKey());
          beanDefinitionRegistry.registerBeanDefinition(objectStoreLocal.getValue(),
                                                        defaultBeanDefinition);
          beanDefinitionRegistry.registerBeanDefinition(objectStoreLocal.getKey(),
                                                        defaultBeanDefinition);
        });
      });
    });

    if (anyBaseStoreWasRedefined.get()) {
      beanDefinitionRegistry
          .registerBeanDefinition(LOCAL_OBJECT_STORE_MANAGER, getBeanDefinitionBuilder(MuleObjectStoreManager.class)
              .addPropertyValue("basePersistentStoreKey", new RuntimeBeanReference(OBJECT_STORE_DEFAULT_PERSISTENT_NAME))
              .addPropertyValue("baseTransientStoreKey", new RuntimeBeanReference(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME))
              .addPropertyValue("basePersistentUserStoreKey", new RuntimeBeanReference(DEFAULT_LOCAL_USER_OBJECT_STORE_NAME))
              .addPropertyValue("baseTransientUserStoreKey",
                                new RuntimeBeanReference(DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME))
              .getBeanDefinition());
    } else {
      beanDefinitionRegistry.registerAlias(OBJECT_STORE_MANAGER, LOCAL_OBJECT_STORE_MANAGER);
    }
  }

  private static BeanDefinition createNotificationManagerBeanDefinition() {
    List<NotificationConfig> defaultNotifications = new ArrayList<>();
    defaultNotifications.add(new NotificationConfig(MuleContextNotificationListener.class, MuleContextNotification.class));
    defaultNotifications.add(new NotificationConfig(SecurityNotificationListener.class, SecurityNotification.class));
    defaultNotifications.add(new NotificationConfig(ManagementNotificationListener.class, ManagementNotification.class));
    defaultNotifications.add(new NotificationConfig(ConnectionNotificationListener.class, ConnectionNotification.class));
    defaultNotifications.add(new NotificationConfig(RegistryNotificationListener.class, RegistryNotification.class));
    defaultNotifications.add(new NotificationConfig(CustomNotificationListener.class, CustomNotification.class));
    defaultNotifications.add(new NotificationConfig(ExceptionNotificationListener.class, ExceptionNotification.class));
    defaultNotifications.add(new NotificationConfig(TransactionNotificationListener.class, TransactionNotification.class));
    return getBeanDefinitionBuilder(ServerNotificationManagerConfigurator.class)
        .addPropertyValue("enabledNotifications", defaultNotifications).getBeanDefinition();
  }

  private void createBootstrapBeanDefinitions() {
    try {
      SpringRegistryBootstrap springRegistryBootstrap =
          new SpringRegistryBootstrap(artifactType, muleContext, optionalObjectsController, this::registerBeanDefinition);
      springRegistryBootstrap.initialise();
    } catch (InitialisationException e) {
      throw new RuntimeException(e);
    }
  }

  private static BeanDefinition getBeanDefinition(Class<?> beanType) {
    return getBeanDefinitionBuilder(beanType).getBeanDefinition();
  }

  private static BeanDefinition getConstantObjectBeanDefinition(Object impl) {
    return getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgValue(impl).getBeanDefinition();
  }

  private static BeanDefinitionBuilder getBeanDefinitionBuilder(Class<?> beanType) {
    return genericBeanDefinition(beanType);
  }

  private static BeanDefinition getBeanDefinition(Class<?> beanType, String factoryMethodName) {
    return getBeanDefinitionBuilder(beanType).setFactoryMethod(factoryMethodName).getBeanDefinition();
  }

}
