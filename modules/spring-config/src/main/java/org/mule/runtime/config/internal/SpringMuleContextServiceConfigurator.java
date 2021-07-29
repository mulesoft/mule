/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_COMPONENT_INITIAL_STATE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
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
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_OBJECT_NAME_PROCESSOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PAYLOAD_STATISTICS_DECORATOR_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_RESOURCE_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STATISTICS;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_GHOST_BUSTER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_FACTORY_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMATION_SERVICE;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.core.internal.interception.InterceptorManager.INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.internal.metadata.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.api.notification.ConnectionNotificationListener;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.ManagementNotification;
import org.mule.runtime.api.notification.ManagementNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.config.internal.NotificationConfig.EnabledNotificationConfig;
import org.mule.runtime.config.internal.dsl.model.config.DefaultComponentInitialStateManager;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.config.internal.factories.ExtensionManagerFactoryBean;
import org.mule.runtime.config.internal.factories.FixedTypeConstantFactoryBean;
import org.mule.runtime.config.internal.factories.MuleContextFactoryBean;
import org.mule.runtime.config.internal.factories.TransactionManagerFactoryBean;
import org.mule.runtime.config.internal.processor.MuleObjectNameProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.internal.cluster.DefaultClusterService;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.connection.DefaultConnectivityTesterFactory;
import org.mule.runtime.core.internal.connection.DelegateConnectionManagerAdapter;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.context.notification.MessageProcessingFlowTraceManager;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.event.DefaultEventContextService;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.exception.MessagingExceptionLocationProvider;
import org.mule.runtime.core.internal.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.internal.management.stats.PayloadStatisticsCursorDecoratorFactory;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.metadata.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.core.internal.policy.DefaultPolicyManager;
import org.mule.runtime.core.internal.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.util.DefaultStreamCloserService;
import org.mule.runtime.core.internal.util.TypeSupplier;
import org.mule.runtime.core.internal.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.module.extension.internal.data.sample.MuleSampleDataService;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxy;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
  private final ResourceLocator resourceLocator;
  private org.mule.runtime.core.internal.registry.Registry originalRegistry;

  private static final ImmutableSet<String> APPLICATION_ONLY_SERVICES = ImmutableSet.<String>builder()
      .add(OBJECT_SECURITY_MANAGER)
      .add(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER)
      .add(OBJECT_MULE_STREAM_CLOSER_SERVICE)
      .add(OBJECT_CONVERTER_RESOLVER)
      .add(OBJECT_PROCESSING_TIME_WATCHER)
      .add(OBJECT_POLICY_MANAGER)
      .add(OBJECT_EXCEPTION_LOCATION_PROVIDER)
      .add(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER)
      .build();

  private static final ImmutableMap<String, String> OBJECT_STORE_NAME_TO_LOCAL_OBJECT_STORE_NAME =
      ImmutableMap.<String, String>builder()
          .put(BASE_IN_MEMORY_OBJECT_STORE_KEY, OBJECT_LOCAL_STORE_IN_MEMORY)
          .put(BASE_PERSISTENT_OBJECT_STORE_KEY, OBJECT_LOCAL_STORE_PERSISTENT)
          .build();

  // Do not use static field. BeanDefinitions are reused and produce weird behaviour
  private final ImmutableMap<String, BeanDefinition> defaultContextServices = ImmutableMap.<String, BeanDefinition>builder()
      .put(OBJECT_TRANSACTION_MANAGER, getBeanDefinition(TransactionManagerFactoryBean.class))
      .put(OBJECT_EXPRESSION_LANGUAGE, getBeanDefinition(MVELExpressionLanguage.class))
      .put(OBJECT_EXPRESSION_MANAGER, getBeanDefinition(DefaultExpressionManagerFactoryBean.class))
      .put(OBJECT_EXTENSION_MANAGER, getBeanDefinition(ExtensionManagerFactoryBean.class))
      .put(OBJECT_TIME_SUPPLIER, getBeanDefinition(LocalTimeSupplier.class))
      .put(OBJECT_CONNECTION_MANAGER, getBeanDefinition(DelegateConnectionManagerAdapter.class))
      .put(METADATA_SERVICE_KEY, getBeanDefinition(MuleMetadataService.class))
      .put(OBJECT_MULE_CONFIGURATION, getBeanDefinition(DefaultMuleConfiguration.class))
      .put(VALUE_PROVIDER_SERVICE_KEY, getBeanDefinition(MuleValueProviderService.class))
      .put(SAMPLE_DATA_SERVICE_KEY, getBeanDefinition(MuleSampleDataService.class))
      .put(OBJECT_TRANSACTION_FACTORY_LOCATOR, getBeanDefinition(TransactionFactoryLocator.class))
      .put(OBJECT_OBJECT_NAME_PROCESSOR, getBeanDefinition(MuleObjectNameProcessor.class))
      .put(OBJECT_POLICY_MANAGER, getBeanDefinition(DefaultPolicyManager.class))
      .put(INTERCEPTOR_MANAGER_REGISTRY_KEY, getBeanDefinition(DefaultProcessorInterceptorManager.class))
      .put(OBJECT_NOTIFICATION_MANAGER, createNotificationManagerBeanDefinition())
      .put(OBJECT_NOTIFICATION_DISPATCHER, getBeanDefinition(DefaultNotificationDispatcher.class))
      .put(NotificationListenerRegistry.REGISTRY_KEY, getBeanDefinition(DefaultNotificationListenerRegistry.class))
      .put(EventContextService.REGISTRY_KEY, getBeanDefinition(DefaultEventContextService.class))
      .put(BASE_IN_MEMORY_OBJECT_STORE_KEY,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_IN_MEMORY)
               .getBeanDefinition())
      .put(OBJECT_LOCAL_STORE_IN_MEMORY,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultInMemoryObjectStore"))
      .put(BASE_PERSISTENT_OBJECT_STORE_KEY,
           getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_PERSISTENT)
               .getBeanDefinition())
      .put(OBJECT_LOCAL_STORE_PERSISTENT,
           getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultPersistentObjectStore"))
      .put(OBJECT_STORE_MANAGER, getPrimaryBeanDefinition(MuleObjectStoreManager.class))
      .put(OBJECT_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class))
      .put(OBJECT_SECURITY_MANAGER, getBeanDefinition(DefaultMuleSecurityManager.class))
      .put(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, getBeanDefinition(MuleMessageProcessingManager.class))
      .put(OBJECT_MULE_STREAM_CLOSER_SERVICE, getBeanDefinition(DefaultStreamCloserService.class))
      .put(OBJECT_CONVERTER_RESOLVER, getBeanDefinition(DynamicDataTypeConversionResolver.class))
      .put(OBJECT_LOCK_FACTORY, getBeanDefinition(MuleLockFactory.class))
      .put(OBJECT_LOCK_PROVIDER, getBeanDefinition(SingleServerLockProvider.class))
      .put(OBJECT_PROCESSING_TIME_WATCHER, getBeanDefinition(DefaultProcessingTimeWatcher.class))
      .put(OBJECT_PAYLOAD_STATISTICS_DECORATOR_FACTORY, getBeanDefinition(PayloadStatisticsCursorDecoratorFactory.class))
      .put(OBJECT_EXCEPTION_LOCATION_PROVIDER, getBeanDefinition(MessagingExceptionLocationProvider.class))
      .put(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER, getBeanDefinition(MessageProcessingFlowTraceManager.class))
      .put(CONNECTIVITY_TESTING_SERVICE_KEY, getBeanDefinition(DefaultConnectivityTestingService.class))
      .put(OBJECT_COMPONENT_INITIAL_STATE_MANAGER, getBeanDefinition(DefaultComponentInitialStateManager.class))
      .put(OBJECT_STREAMING_MANAGER, getBeanDefinition(DefaultStreamingManager.class))
      .put(OBJECT_STREAMING_GHOST_BUSTER, getBeanDefinition(StreamingGhostBuster.class))
      .put(OBJECT_TRANSFORMATION_SERVICE, getBeanDefinition(ExtendedTransformationService.class))
      .put(OBJECT_SCHEDULER_POOLS_CONFIG, getConstantObjectBeanDefinition(SchedulerContainerPoolsConfig.getInstance()))
      .put(OBJECT_SCHEDULER_BASE_CONFIG, getBeanDefinition(SchedulerBaseConfigFactory.class))
      .put(OBJECT_CLUSTER_SERVICE, getBeanDefinition(DefaultClusterService.class))
      .put(OBJECT_CONNECTIVITY_TESTER_FACTORY, getBeanDefinition(DefaultConnectivityTesterFactory.class))
      .put(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, getBeanDefinition(NoOpLazyComponentInitializer.class))
      .put(METADATA_CACHE_MANAGER_KEY, getBeanDefinition(DefaultPersistentMetadataCacheManager.class))
      .put(MULE_PROFILING_SERVICE_KEY, getBeanDefinition(DefaultProfilingService.class))
      .build();

  private final SpringConfigurationComponentLocator componentLocator;
  private final ConfigurationProperties configurationProperties;
  private final Registry serviceLocator;

  public SpringMuleContextServiceConfigurator(MuleContext muleContext,
                                              ConfigurationProperties configurationProperties,
                                              ArtifactType artifactType,
                                              OptionalObjectsController optionalObjectsController,
                                              BeanDefinitionRegistry beanDefinitionRegistry,
                                              SpringConfigurationComponentLocator componentLocator,
                                              Registry serviceLocator,
                                              org.mule.runtime.core.internal.registry.Registry originalRegistry,
                                              ResourceLocator resourceLocator) {
    this.muleContext = muleContext;
    this.configurationProperties = configurationProperties;
    this.customServiceRegistry = (CustomServiceRegistry) muleContext.getCustomizationService();
    this.artifactType = artifactType;
    this.optionalObjectsController = optionalObjectsController;
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    this.componentLocator = componentLocator;
    this.serviceLocator = serviceLocator;
    this.originalRegistry = originalRegistry;
    this.resourceLocator = resourceLocator;
  }

  void createArtifactServices() {

    registerBeanDefinition(OBJECT_MULE_CONTEXT, createMuleContextDefinition());
    registerConstantBeanDefinition(DEFAULT_OBJECT_SERIALIZER_NAME, muleContext.getObjectSerializer());
    registerConstantBeanDefinition(OBJECT_CONFIGURATION_PROPERTIES, configurationProperties);
    registerConstantBeanDefinition(ErrorTypeRepository.class.getName(), new ContributedErrorTypeRepository());
    registerConstantBeanDefinition(ErrorTypeLocator.class.getName(), new ContributedErrorTypeLocator());
    registerConstantBeanDefinition(ConfigurationComponentLocator.REGISTRY_KEY, componentLocator);
    registerConstantBeanDefinition(OBJECT_NOTIFICATION_HANDLER, muleContext.getNotificationManager());
    registerConstantBeanDefinition(OBJECT_REGISTRY, serviceLocator);
    registerConstantBeanDefinition(OBJECT_STATISTICS, muleContext.getStatistics());
    registerConstantBeanDefinition(OBJECT_RESOURCE_LOCATOR, resourceLocator);
    loadServiceConfigurators();

    defaultContextServices.entrySet().stream()
        .filter(service -> !APPLICATION_ONLY_SERVICES.contains(service.getKey()) || artifactType.equals(APP)
            || artifactType.equals(POLICY))
        .forEach(service -> registerBeanDefinition(service.getKey(), service.getValue()));

    createBootstrapBeanDefinitions();
    createLocalObjectStoreBeanDefinitions();
    createLocalLockFactoryBeanDefinitions();
    createQueueManagerBeanDefinitions();
    createCustomServices();
    absorbOriginalRegistry();
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
      final BeanDefinition beanDefinition = getCustomServiceBeanDefinition(customService, serviceName);

      registerBeanDefinition(serviceName, beanDefinition);
    }
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

  private void createQueueManagerBeanDefinitions() {
    AtomicBoolean customManagerDefined = new AtomicBoolean(false);
    customServiceRegistry.getOverriddenService(OBJECT_QUEUE_MANAGER).ifPresent(customService -> {
      customManagerDefined.set(true);
      registerBeanDefinition(OBJECT_QUEUE_MANAGER, getCustomServiceBeanDefinition(customService, OBJECT_QUEUE_MANAGER));
    });

    if (customManagerDefined.get()) {
      registerBeanDefinition(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class));
    } else {
      beanDefinitionRegistry.registerAlias(OBJECT_QUEUE_MANAGER, OBJECT_LOCAL_QUEUE_MANAGER);
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

  private void createLocalLockFactoryBeanDefinitions() {
    AtomicBoolean customLockFactoryWasDefined = new AtomicBoolean(false);
    customServiceRegistry.getOverriddenService(OBJECT_LOCK_FACTORY).ifPresent(customService -> {
      customLockFactoryWasDefined.set(true);
      beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCK_FACTORY,
                                                    getCustomServiceBeanDefinition(customService, OBJECT_LOCK_FACTORY));
    });

    if (customLockFactoryWasDefined.get()) {
      beanDefinitionRegistry
          .registerBeanDefinition(LOCAL_OBJECT_LOCK_FACTORY, defaultContextServices.get(OBJECT_LOCK_FACTORY));
    } else {
      beanDefinitionRegistry.registerAlias(OBJECT_LOCK_FACTORY, LOCAL_OBJECT_LOCK_FACTORY);
    }
  }

  private void createLocalObjectStoreBeanDefinitions() {
    AtomicBoolean anyBaseStoreWasRedefined = new AtomicBoolean(false);
    OBJECT_STORE_NAME_TO_LOCAL_OBJECT_STORE_NAME.entrySet().forEach(objectStoreLocal -> customServiceRegistry
        .getOverriddenService(objectStoreLocal.getKey()).ifPresent(customService -> {
          anyBaseStoreWasRedefined.set(true);
          beanDefinitionRegistry.registerBeanDefinition(objectStoreLocal.getKey(), getCustomServiceBeanDefinition(customService,
                                                                                                                  objectStoreLocal
                                                                                                                      .getKey()));
        }));

    if (anyBaseStoreWasRedefined.get()) {
      final AbstractBeanDefinition beanDefinition = getBeanDefinitionBuilder(MuleObjectStoreManager.class)
          .addPropertyValue("basePersistentStoreKey", OBJECT_LOCAL_STORE_PERSISTENT)
          .addPropertyValue("baseTransientStoreKey", OBJECT_LOCAL_STORE_IN_MEMORY)
          .getBeanDefinition();
      beanDefinition.setPrimary(false);
      beanDefinitionRegistry.registerBeanDefinition(LOCAL_OBJECT_STORE_MANAGER, beanDefinition);
    } else {
      beanDefinitionRegistry.registerAlias(OBJECT_STORE_MANAGER, LOCAL_OBJECT_STORE_MANAGER);
    }
  }

  private static BeanDefinition createNotificationManagerBeanDefinition() {
    return getBeanDefinitionBuilder(ServerNotificationManagerConfigurator.class)
        .addPropertyValue("enabledNotifications", ImmutableList
            .<NotificationConfig<? extends Notification, ? extends NotificationListener>>builder()
            .add(new EnabledNotificationConfig<>(MuleContextNotificationListener.class, MuleContextNotification.class))
            .add(new EnabledNotificationConfig<>(SecurityNotificationListener.class, SecurityNotification.class))
            .add(new EnabledNotificationConfig<>(ManagementNotificationListener.class, ManagementNotification.class))
            .add(new EnabledNotificationConfig<>(ConnectionNotificationListener.class, ConnectionNotification.class))
            .add(new EnabledNotificationConfig<>(CustomNotificationListener.class, CustomNotification.class))
            .add(new EnabledNotificationConfig<>(ExceptionNotificationListener.class, ExceptionNotification.class))
            .add(new EnabledNotificationConfig<>(TransactionNotificationListener.class, TransactionNotification.class))
            .add(new EnabledNotificationConfig<>(ExtensionNotificationListener.class, ExtensionNotification.class))
            .build())
        .getBeanDefinition();
  }

  private BeanDefinition createMuleContextDefinition() {
    return getBeanDefinitionBuilder(MuleContextFactoryBean.class)
        .addConstructorArgValue(muleContext)
        .getBeanDefinition();
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

  private static BeanDefinition getPrimaryBeanDefinition(Class<?> beanType) {
    BeanDefinition beanDefinition = getBeanDefinition(beanType);
    beanDefinition.setPrimary(true);

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

  private static BeanDefinition getBeanDefinition(Class<?> beanType, String factoryMethodName) {
    return getBeanDefinitionBuilder(beanType).setFactoryMethod(factoryMethodName).getBeanDefinition();
  }

}
