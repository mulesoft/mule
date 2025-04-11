/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.builders;


import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_COMPONENT_TRACER_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EVENT_TRACER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ERROR_METRICS_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METER_PROVIDER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SPAN_EXPORTER_CONFIGURATION_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_ARTIFACT_ENCODING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_RESOURCE_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_GHOST_BUSTER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_FACTORY_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMATION_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMERS_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMER_RESOLVER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.builders.RegistryBootstrap.defaultRegistryBoostrap;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory.getDummyComponentTracerFactory;
import static org.mule.runtime.core.internal.profiling.NoopCoreEventTracer.getNoopCoreEventTracer;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultInMemoryObjectStore;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultPersistentObjectStore;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.internal.cluster.DefaultClusterService;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultArtifactEncoding;
import org.mule.runtime.core.internal.config.DefaultResourceLocator;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.connection.DefaultConnectivityTesterFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.internal.profiling.EmptySpanExporterConfiguration;
import org.mule.runtime.core.internal.profiling.NoOpProfilingService;
import org.mule.runtime.core.internal.profiling.NoopSpanExporterFactory;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.transformer.ExtendedTransformationService;
import org.mule.runtime.core.internal.transformer.TransformersRegistry;
import org.mule.runtime.core.internal.util.DefaultStreamCloserService;
import org.mule.runtime.core.internal.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * Configures a {@link MuleContext} {@link Registry} with the bare minimum elements needed for functioning. This instance will
 * configure the elements related to a particular {@link MuleContext} only. It will not configure container related elements such
 * as {@link Service mule services}.
 *
 * @return a {@link ConfigurationBuilder}
 * @since 4.5.0
 */
public class MinimalConfigurationBuilder extends AbstractConfigurationBuilder {

  private final Set<String> registeredServices = new HashSet<>();

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));

    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();

    defaultRegistryBoostrap(APP, muleContext.getRegistryBootstrapServiceDiscoverer(), (n, o) -> registerObject(n, o, muleContext))
        .initialise();

    configureQueueManager(muleContext);

    registry.registerObject(OBJECT_MULE_CONTEXT, muleContext);
    // registry.registerObject(OBJECT_MULE_CONFIGURATION, muleContext.getConfiguration());

    registerCustomServices(muleContext);
    registerObjectStoreManager(muleContext);
    registerSchedulerPoolsConfig(muleContext);
    registerLockFactory(muleContext);
    registerTransformerRegistry(muleContext);
    registerExpressionManager(muleContext, registry);
    registerObject(OBJECT_ARTIFACT_ENCODING, new DefaultArtifactEncoding(muleContext.getConfiguration().getDefaultEncoding()),
                   muleContext);
    registerConnectionManager(muleContext);
    registerNotificationHandlingObjects(muleContext);
    registerConnectivityTester(muleContext);
    registerInterceptionApiObjects(muleContext);

    registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager(), muleContext);
    registerObject(OBJECT_MULE_STREAM_CLOSER_SERVICE, new DefaultStreamCloserService(), muleContext);
    registerObject(DEFAULT_OBJECT_SERIALIZER_NAME, new JavaObjectSerializer(muleContext.getExecutionClassLoader()), muleContext);

    final ContributedErrorTypeRepository contributedErrorTypeRepository = new ContributedErrorTypeRepository();
    registerObject(ErrorTypeRepository.class.getName(), contributedErrorTypeRepository, muleContext);
    final ContributedErrorTypeLocator contributedErrorTypeLocator = new ContributedErrorTypeLocator();
    contributedErrorTypeLocator.setDelegate(createDefaultErrorTypeLocator(contributedErrorTypeRepository));
    registerObject(ErrorTypeLocator.class.getName(), contributedErrorTypeLocator, muleContext);

    registerObject(OBJECT_STREAMING_GHOST_BUSTER, new StreamingGhostBuster(), muleContext);
    registerStreamingManager(muleContext);
    registerObject(OBJECT_TIME_SUPPLIER, new LocalTimeSupplier(), muleContext);
    registerObject(OBJECT_CLUSTER_SERVICE, new DefaultClusterService(), muleContext);

    registerTransactionFactoryLocator(muleContext);

    // This is overridden only if no other test configurator has set the profiling service.
    if (((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(MULE_PROFILING_SERVICE_KEY) == null) {
      registerObject(MULE_PROFILING_SERVICE_KEY, new NoOpProfilingService(), muleContext);
    }
    configureCoreTracer(muleContext);
    configureComponentTracerFactory(muleContext);
    configureSpanExporterConfiguration(muleContext);
    configureSpanExporterFactory(muleContext);

    configureErrorMetricsFactory(muleContext);
    configureBaseArtifactMeterProvider(muleContext);

    registerObject(ComponentInitialStateManager.SERVICE_ID, new ComponentInitialStateManager() {

      @Override
      public boolean mustStartMessageSource(Component component) {
        return true;
      }
    }, muleContext);
    registerObject(OBJECT_RESOURCE_LOCATOR, new DefaultResourceLocator(), muleContext);

    for (String serviceId : ((InternalCustomizationService) muleContext.getCustomizationService()).getDefaultServices()
        .keySet()) {
      if (!registeredServices.contains(serviceId)) {
        registerObject(serviceId, null, muleContext);
      }
    }
  }

  protected void configureBaseArtifactMeterProvider(MuleContext muleContext) throws RegistrationException {
    registerObject(MULE_METER_PROVIDER_KEY, MeterProvider.NO_OP, muleContext);
  }

  protected void configureErrorMetricsFactory(MuleContext muleContext) throws RegistrationException {
    registerObject(MULE_ERROR_METRICS_FACTORY_KEY, ErrorMetricsFactory.NO_OP, muleContext);
  }

  protected void registerTransactionFactoryLocator(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_TRANSACTION_FACTORY_LOCATOR, new TransactionFactoryLocator(), muleContext);
  }

  protected void registerStreamingManager(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_STREAMING_MANAGER, new DefaultStreamingManager(), muleContext);
  }

  protected void registerInterceptionApiObjects(MuleContext muleContext) throws RegistrationException {
    registerObject(INTERCEPTOR_MANAGER_REGISTRY_KEY, new DefaultProcessorInterceptorManager(), muleContext);
  }

  protected void registerConnectivityTester(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_CONNECTIVITY_TESTER_FACTORY, new DefaultConnectivityTesterFactory(), muleContext);
  }

  protected void registerConnectionManager(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_CONNECTION_MANAGER, new DefaultConnectionManager(muleContext), muleContext);
  }

  protected void registerNotificationHandlingObjects(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_NOTIFICATION_DISPATCHER, new DefaultNotificationDispatcher(), muleContext);
    registerObject(NotificationListenerRegistry.REGISTRY_KEY, new DefaultNotificationListenerRegistry(), muleContext);
  }

  protected void registerExpressionManager(MuleContext muleContext, MuleRegistry registry) throws MuleException {
    registerObject(OBJECT_EXPRESSION_MANAGER, getExpressionManager(muleContext, registry), muleContext);
  }

  protected ExtendedExpressionManager getExpressionManager(MuleContext muleContext, MuleRegistry registry)
      throws MuleException {
    DefaultExpressionManager expressionManager = new DefaultExpressionManager();
    DefaultExpressionLanguageFactoryService service = getExpressionLanguageFactoryService(registry);
    ArtifactEncoding artifactEncoding = getArtifactEncoding(registry);
    expressionManager.setExpressionLanguage(new DataWeaveExpressionLanguageAdaptor(muleContext,
                                                                                   null,
                                                                                   muleContext.getConfiguration(),
                                                                                   artifactEncoding,
                                                                                   service,
                                                                                   null));

    muleContext.getInjector().inject(expressionManager);

    return expressionManager;
  }

  protected DefaultExpressionLanguageFactoryService getExpressionLanguageFactoryService(MuleRegistry registry)
      throws RegistrationException {
    return registry.lookupObject(DefaultExpressionLanguageFactoryService.class);
  }

  protected ArtifactEncoding getArtifactEncoding(MuleRegistry registry)
      throws RegistrationException {
    return registry.lookupObject(ArtifactEncoding.class);
  }

  protected void registerTransformerRegistry(MuleContext muleContext) throws RegistrationException {
    TransformersRegistry transformersRegistry = new DefaultTransformersRegistry();
    registerObject(OBJECT_TRANSFORMERS_REGISTRY, transformersRegistry, muleContext);
    registerObject(OBJECT_CONVERTER_RESOLVER, new DynamicDataTypeConversionResolver(transformersRegistry), muleContext);
    registerObject(OBJECT_TRANSFORMATION_SERVICE, new ExtendedTransformationService(), muleContext);
    registerObject(OBJECT_TRANSFORMER_RESOLVER, new TypeBasedTransformerResolver(), muleContext);
  }

  protected void registerLockFactory(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_LOCK_PROVIDER, new SingleServerLockProvider(), muleContext);
    registerObject(OBJECT_LOCK_FACTORY, new MuleLockFactory(), muleContext);
  }

  protected void registerSchedulerPoolsConfig(MuleContext muleContext) throws RegistrationException {
    registerObject(OBJECT_SCHEDULER_POOLS_CONFIG, SchedulerContainerPoolsConfig.getInstance(), muleContext);
    registerObject(OBJECT_SCHEDULER_BASE_CONFIG,
                   config().withPrefix(muleContext.getConfiguration().getId())
                       .withShutdownTimeout(() -> muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS),
                   muleContext);
  }

  protected void registerCustomServices(MuleContext muleContext) {
    for (Entry<String, CustomService> entry : ((InternalCustomizationService) (muleContext.getCustomizationService()))
        .getCustomServices()
        .entrySet()) {
      entry.getValue().getServiceImpl().ifPresent(s -> {
        try {
          registerObject(entry.getKey(), s, muleContext);
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
      });
    }
  }

  protected <T> void registerObject(String serviceId, T defaultServiceImpl, MuleContext muleContext)
      throws RegistrationException {
    registeredServices.add(serviceId);

    Optional<T> serviceImpl =
        ((InternalCustomizationService) muleContext.getCustomizationService()).getOverriddenService(serviceId)
            .map(customService -> customService.getServiceImpl(defaultServiceImpl))
            .orElse(ofNullable(defaultServiceImpl));

    if (!serviceImpl.isPresent()) {
      return;
    }

    var service = serviceImpl.orElseThrow();
    if (service instanceof MuleContextAware) {
      ((MuleContextAware) service).setMuleContext(muleContext);
    }
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(serviceId, service);
  }

  protected void registerObjectStoreManager(MuleContext muleContext) throws RegistrationException {
    registerObjectStorePartitions(muleContext);

    MuleObjectStoreManager osm = new MuleObjectStoreManager();
    osm.setBasePersistentStoreKey(BASE_PERSISTENT_OBJECT_STORE_KEY);
    osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
    osm.setMuleContext(muleContext);
    registerObject(OBJECT_STORE_MANAGER, osm, muleContext);
  }

  protected void registerObjectStorePartitions(MuleContext muleContext) throws RegistrationException {
    registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY, getDefaultInMemoryObjectStore(), muleContext);
    registerObject(BASE_PERSISTENT_OBJECT_STORE_KEY, getDefaultPersistentObjectStore(), muleContext);
  }

  protected ObjectStore<Serializable> getDefaultPersistentObjectStore() {
    return createDefaultPersistentObjectStore();
  }

  protected ObjectStore<Serializable> getDefaultInMemoryObjectStore() {
    return createDefaultInMemoryObjectStore();
  }

  protected void configureQueueManager(MuleContext muleContext) throws RegistrationException {
    var queueManager = new TransactionalQueueManager();
    queueManager.setMuleConfiguration(muleContext.getConfiguration());
    queueManager.setMuleContext(muleContext);
    registerObject(OBJECT_QUEUE_MANAGER, queueManager, muleContext);
    registerObject(LOCAL_QUEUE_MANAGER_KEY, queueManager, muleContext);
  }

  private void configureComponentTracerFactory(MuleContext muleContext) throws RegistrationException {
    ComponentTracerFactory<CoreEvent> componentTracerFactory = getDummyComponentTracerFactory();
    registerObject(MULE_CORE_COMPONENT_TRACER_FACTORY_KEY, componentTracerFactory, muleContext);
  }

  protected void configureCoreTracer(MuleContext muleContext) throws RegistrationException {
    EventTracer<CoreEvent> tracer = getNoopCoreEventTracer();
    registerObject(MULE_CORE_EVENT_TRACER_KEY, tracer, muleContext);
  }

  protected void configureSpanExporterFactory(MuleContext muleContext) throws RegistrationException {
    SpanExporterFactory spanExporterFactory = new NoopSpanExporterFactory();
    registerObject(MULE_CORE_EXPORTER_FACTORY_KEY, spanExporterFactory, muleContext);
  }

  protected void configureSpanExporterConfiguration(MuleContext muleContext) throws RegistrationException {
    registerObject(MULE_SPAN_EXPORTER_CONFIGURATION_KEY, new EmptySpanExporterConfiguration(), muleContext);
  }

}
