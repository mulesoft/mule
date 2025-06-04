/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.config.custom.ServiceConfigurator.lookupServiceConfigurators;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_ADD_ARTIFACT_AST_TO_REGISTRY_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.FORWARD_COMPATIBILITY_HELPER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ARTIFACT_METER_PROVIDER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_COMPONENT_TRACER_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EVENT_TRACER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_SPAN_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ERROR_METRICS_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEMORY_MANAGEMENT_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METER_EXPORTER_CONFIGURATION_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METER_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METER_PROVIDER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SPAN_EXPORTER_CONFIGURATION_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_TRACER_INITIAL_SPAN_INFO_PROVIDER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_TRACING_LEVEL_CONFIGURATION_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_ARTIFACT_AST;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_COMPONENT_INITIAL_STATE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXCEPTION_LOCATION_PROVIDER;
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
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_OBJECT_NAME_PROCESSOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_RESOURCE_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STATISTICS;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_GHOST_BUSTER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_FACTORY_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMATION_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.SDK_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.BINDING_PROVIDER_PREDICATE;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.TRANSFORMER_PREDICATE;
import static org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider.CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY;
import static org.mule.runtime.feature.api.management.FeatureFlaggingManagementService.PROFILING_FEATURE_MANAGEMENT_SERVICE_KEY;
import static org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory.METADATA_CACHE_ID_GENERATOR_KEY;
import static org.mule.runtime.metadata.internal.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.mule.runtime.metrics.exporter.api.MeterExporterProperties.METRIC_EXPORTER_ENABLED_PROPERTY;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.getBoolean;
import static java.lang.Boolean.valueOf;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.config.internal.factories.ExtensionManagerFactoryBean;
import org.mule.runtime.config.internal.factories.MuleConfigurationConfigurator;
import org.mule.runtime.config.internal.factories.MuleContextFactoryBean;
import org.mule.runtime.config.internal.factories.TransactionManagerFactoryBean;
import org.mule.runtime.config.internal.model.dsl.config.DefaultComponentInitialStateManager;
import org.mule.runtime.config.internal.processor.MuleObjectNameProcessor;
import org.mule.runtime.config.internal.registry.SpringRegistryBootstrap;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.management.stats.ArtifactMeterProvider;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.internal.cluster.DefaultClusterService;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultFeatureManagementService;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.core.internal.connection.DefaultConnectivityTesterFactory;
import org.mule.runtime.core.internal.connection.DelegateConnectionManagerAdapter;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.context.notification.MessageProcessingFlowTraceManager;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.event.DefaultEventContextService;
import org.mule.runtime.core.internal.exception.MessagingExceptionLocationProvider;
import org.mule.runtime.core.internal.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.internal.policy.DefaultPolicyManager;
import org.mule.runtime.core.internal.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.internal.profiling.ProfilingServiceWrapper;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.transformer.ExtendedTransformationService;
import org.mule.runtime.core.internal.util.DefaultStreamCloserService;
import org.mule.runtime.core.internal.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.metadata.internal.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory;
import org.mule.runtime.metrics.exporter.impl.optel.config.OpenTelemetryAutoConfigurableMeterExporterConfiguration;
import org.mule.runtime.metrics.impl.DefaultMeterProvider;
import org.mule.runtime.metrics.impl.meter.error.DefaultErrorMetricsFactory;
import org.mule.runtime.module.extension.api.runtime.compatibility.DefaultForwardCompatibilityHelper;
import org.mule.runtime.module.extension.internal.data.sample.MuleSampleDataService;
import org.mule.runtime.module.extension.internal.store.SdkObjectStoreManagerAdapter;
import org.mule.runtime.tracer.customization.impl.provider.DefaultInitialSpanInfoProvider;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterFactory;
import org.mule.runtime.tracer.exporter.impl.optel.config.OpenTelemetryAutoConfigurableSpanExporterConfiguration;
import org.mule.runtime.tracer.impl.CoreEventComponentTracerFactory;
import org.mule.runtime.tracer.impl.SelectableCoreEventTracer;
import org.mule.runtime.tracer.impl.span.factory.ExecutionSpanFactory;
import org.mule.runtime.tracing.level.impl.config.FileTracingLevelConfiguration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * This class configured all the services available in a {@code MuleContext}.
 * <p>
 * This class takes cares of registering bean definitions for each of the provided services so dependency injection can be
 * properly done through the use of {@link Inject}.
 *
 * @since 4.0
 */
public class SpringMuleContextServiceConfigurator extends AbstractSpringMuleContextServiceConfigurator {

  private static final String OBJECT_FLOW_CLASSIFIER = "_muleFlowClassifier";

  private final MuleContextWithRegistry muleContext;
  private final ArtifactType artifactType;
  private final ArtifactAst artifactAst;
  private final ResourceLocator resourceLocator;

  private static final ImmutableSet<String> APPLICATION_ONLY_SERVICES = ImmutableSet.<String>builder()
      .add(OBJECT_SECURITY_MANAGER)
      .add(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER)
      .add(OBJECT_MULE_STREAM_CLOSER_SERVICE)
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
      .put(OBJECT_EXTENSION_MANAGER, getBeanDefinition(ExtensionManagerFactoryBean.class))
      .put(OBJECT_TIME_SUPPLIER, getBeanDefinition(LocalTimeSupplier.class))
      .put(OBJECT_CONNECTION_MANAGER, getBeanDefinition(DelegateConnectionManagerAdapter.class))
      .put(OBJECT_MULE_CONFIGURATION, getBeanDefinition(MuleConfigurationConfigurator.class))
      .put(OBJECT_TRANSACTION_FACTORY_LOCATOR, getBeanDefinition(TransactionFactoryLocator.class))
      .put(OBJECT_OBJECT_NAME_PROCESSOR, getBeanDefinition(MuleObjectNameProcessor.class))
      .put(OBJECT_POLICY_MANAGER, getBeanDefinition(DefaultPolicyManager.class))
      .put(INTERCEPTOR_MANAGER_REGISTRY_KEY, getBeanDefinition(DefaultProcessorInterceptorManager.class))
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
      .put(SDK_OBJECT_STORE_MANAGER, getPrimaryBeanDefinition(SdkObjectStoreManagerAdapter.class))
      .put(OBJECT_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class))
      .put(OBJECT_SECURITY_MANAGER, getBeanDefinition(DefaultMuleSecurityManager.class))
      .put(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, getBeanDefinition(MuleMessageProcessingManager.class))
      .put(OBJECT_MULE_STREAM_CLOSER_SERVICE, getBeanDefinition(DefaultStreamCloserService.class))
      .put(OBJECT_CONVERTER_RESOLVER, getBeanDefinition(DynamicDataTypeConversionResolver.class))
      .put(OBJECT_LOCK_FACTORY, getBeanDefinition(MuleLockFactory.class))
      .put(OBJECT_LOCK_PROVIDER, getBeanDefinition(SingleServerLockProvider.class))
      .put(OBJECT_PROCESSING_TIME_WATCHER, getBeanDefinition(DefaultProcessingTimeWatcher.class))
      .put(OBJECT_EXCEPTION_LOCATION_PROVIDER, getBeanDefinition(MessagingExceptionLocationProvider.class))
      .put(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER, getBeanDefinition(MessageProcessingFlowTraceManager.class))
      .put(OBJECT_COMPONENT_INITIAL_STATE_MANAGER, getBeanDefinition(DefaultComponentInitialStateManager.class))
      .put(OBJECT_STREAMING_MANAGER, getBeanDefinition(DefaultStreamingManager.class))
      .put(OBJECT_STREAMING_GHOST_BUSTER, getBeanDefinition(StreamingGhostBuster.class))
      .put(OBJECT_TRANSFORMATION_SERVICE, getBeanDefinition(ExtendedTransformationService.class))
      .put(OBJECT_CLUSTER_SERVICE, getBeanDefinition(DefaultClusterService.class))
      .put(OBJECT_CONNECTIVITY_TESTER_FACTORY, getBeanDefinition(DefaultConnectivityTesterFactory.class))
      .put(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, getBeanDefinition(NoOpLazyComponentInitializer.class))
      .put(MULE_SPAN_EXPORTER_CONFIGURATION_KEY, getBeanDefinition(OpenTelemetryAutoConfigurableSpanExporterConfiguration.class))
      .put(MULE_PROFILING_SERVICE_KEY, getBeanDefinitionForProfilingService())
      .put(MULE_CORE_SPAN_FACTORY_KEY, getBeanDefinition(ExecutionSpanFactory.class))
      .put(MULE_CORE_EXPORTER_FACTORY_KEY, getBeanDefinition(OpenTelemetrySpanExporterFactory.class))
      .put(MULE_CORE_EVENT_TRACER_KEY, getBeanDefinition(SelectableCoreEventTracer.class))
      .put(MULE_CORE_COMPONENT_TRACER_FACTORY_KEY, getBeanDefinition(CoreEventComponentTracerFactory.class))
      .put(MULE_ERROR_METRICS_FACTORY_KEY, resolveErrorMetricsFactory())
      .put(MULE_METER_PROVIDER_KEY, resolveBaseArtifactMeterProvider())
      .put(MULE_METER_EXPORTER_CONFIGURATION_KEY,
           getBeanDefinition(OpenTelemetryAutoConfigurableMeterExporterConfiguration.class))
      .put(MULE_METER_EXPORTER_FACTORY_KEY, getBeanDefinition(OpenTelemetryMeterExporterFactory.class))
      .put(MULE_TRACING_LEVEL_CONFIGURATION_KEY, getBeanDefinition(FileTracingLevelConfiguration.class))
      .put(MULE_TRACER_INITIAL_SPAN_INFO_PROVIDER_KEY, getBeanDefinition(DefaultInitialSpanInfoProvider.class))
      .put(PROFILING_FEATURE_MANAGEMENT_SERVICE_KEY, getBeanDefinition(DefaultFeatureManagementService.class))
      .put(FORWARD_COMPATIBILITY_HELPER_KEY, getBeanDefinition(DefaultForwardCompatibilityHelper.class))
      .build();

  // Do not use static field. BeanDefinitions are reused and produce weird behaviour
  private final ImmutableMap<String, BeanDefinition> toolingContextServices = ImmutableMap.<String, BeanDefinition>builder()
      .put(METADATA_SERVICE_KEY, getBeanDefinition(MuleMetadataService.class))
      .put(VALUE_PROVIDER_SERVICE_KEY, getBeanDefinition(MuleValueProviderService.class))
      .put(SAMPLE_DATA_SERVICE_KEY, getBeanDefinition(MuleSampleDataService.class))
      .put(CONNECTIVITY_TESTING_SERVICE_KEY, getBeanDefinition(DefaultConnectivityTestingService.class))
      .put(METADATA_CACHE_MANAGER_KEY, getBeanDefinition(DefaultPersistentMetadataCacheManager.class))
      .put(METADATA_CACHE_ID_GENERATOR_KEY, getBeanDefinition(ModelBasedMetadataCacheIdGeneratorFactory.class))
      .build();

  private final MuleFunctionsBindingContextProvider coreFunctionsProvider;
  private final ConfigurationProperties configurationProperties;
  private final Map<String, String> artifactProperties;
  private final boolean addToolingObjectsToRegistry;
  private final MemoryManagementService memoryManagementService;

  public SpringMuleContextServiceConfigurator(MuleContextWithRegistry muleContext,
                                              MuleFunctionsBindingContextProvider coreFunctionsProvider,
                                              ConfigurationProperties configurationProperties,
                                              Map<String, String> artifactProperties,
                                              boolean addToolingObjectsToRegistry,
                                              ArtifactType artifactType,
                                              ArtifactAst artifactAst,
                                              BeanDefinitionRegistry beanDefinitionRegistry,
                                              Registry serviceLocator,
                                              ResourceLocator resourceLocator,
                                              MemoryManagementService memoryManagementService) {
    super((InternalCustomizationService) muleContext.getCustomizationService(), beanDefinitionRegistry, serviceLocator);

    this.muleContext = muleContext;
    this.coreFunctionsProvider = coreFunctionsProvider;
    this.configurationProperties = configurationProperties;
    this.artifactProperties = artifactProperties;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;
    this.artifactType = artifactType;
    this.artifactAst = artifactAst;
    this.resourceLocator = resourceLocator;
    this.memoryManagementService = memoryManagementService;
  }

  private BeanDefinition getBeanDefinitionForProfilingService() {
    return getBeanDefinition(ProfilingServiceWrapper.class);
  }

  protected void createArtifactServices() {
    registerBeanDefinition(OBJECT_MULE_CONTEXT, createMuleContextDefinition());
    registerConstantBeanDefinition(DEFAULT_OBJECT_SERIALIZER_NAME, muleContext.getObjectSerializer());
    // This configurationProperties will contain the properties configured in the app itself, taking precedence over the base one
    // from the base registry
    registerConstantBeanDefinition(OBJECT_CONFIGURATION_PROPERTIES, configurationProperties);
    registerConstantBeanDefinition(OBJECT_NOTIFICATION_HANDLER, muleContext.getNotificationManager());
    registerConstantBeanDefinition(OBJECT_FLOW_CLASSIFIER, new FlowClassifierFactory(artifactAst).create());
    registerConstantBeanDefinition(OBJECT_STATISTICS, muleContext.getStatistics());
    registerConstantBeanDefinition(OBJECT_RESOURCE_LOCATOR, resourceLocator);
    registerConstantBeanDefinition(MULE_MEMORY_MANAGEMENT_SERVICE, memoryManagementService);
    registerBeanDefinition(MULE_ARTIFACT_METER_PROVIDER_KEY, resolveArtifactIdMeterProvider());
    loadServiceConfigurators();

    registerContextServices(defaultContextServices);
    if (isAddToolingObjectsToRegistry()) {
      registerContextServices(toolingContextServices);
    }

    createBootstrapBeanDefinitions();
    createLocalObjectStoreBeanDefinitions();
    createLocalLockFactoryBeanDefinitions();
    createQueueManagerBeanDefinitions();
    createCustomServices();

    coreFunctionsProvider.setConfigurationProperties(configurationProperties);
    registerConstantBeanDefinition(CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY, coreFunctionsProvider);

    artifactProperties.forEach(this::registerConstantBeanDefinition);

    if (valueOf(artifactProperties.getOrDefault(MULE_ADD_ARTIFACT_AST_TO_REGISTRY_DEPLOYMENT_PROPERTY, FALSE.toString()))) {
      registerConstantBeanDefinition(OBJECT_ARTIFACT_AST, artifactAst);
    }
  }

  private void registerContextServices(Map<String, BeanDefinition> contextServices) {
    contextServices.entrySet().stream()
        .filter(service -> !APPLICATION_ONLY_SERVICES.contains(service.getKey()) || artifactType.equals(APP)
            || artifactType.equals(POLICY))
        .forEach(service -> registerBeanDefinition(service.getKey(), service.getValue()));
  }

  private void loadServiceConfigurators() {
    lookupServiceConfigurators(this.getClass().getClassLoader())
        .forEach(customizationInfo -> customizationInfo.configure(getCustomizationService()));
  }

  private void createCustomServices() {
    final Map<String, CustomService> customServices = getCustomizationService().getCustomServices();
    for (String serviceName : customServices.keySet()) {

      if (containsBeanDefinition(serviceName)) {
        throw new IllegalStateException("There is already a bean definition registered with key: " + serviceName);
      }

      final CustomService customService = customServices.get(serviceName);

      // TODO MULE-19927 avoid this filter
      if (!isServiceRuntimeProvided(customService)) {
        final BeanDefinition beanDefinition = getCustomServiceBeanDefinition(customService, serviceName);

        registerBeanDefinition(serviceName, beanDefinition);
      }
    }
  }

  private void createQueueManagerBeanDefinitions() {
    AtomicBoolean customManagerDefined = new AtomicBoolean(false);
    getCustomizationService().getOverriddenService(OBJECT_QUEUE_MANAGER).ifPresent(customService -> {
      customManagerDefined.set(true);
      registerBeanDefinition(OBJECT_QUEUE_MANAGER, getCustomServiceBeanDefinition(customService, OBJECT_QUEUE_MANAGER));
    });

    if (customManagerDefined.get()) {
      registerBeanDefinition(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinition(TransactionalQueueManager.class));
    } else {
      getBeanDefinitionRegistry().registerAlias(OBJECT_QUEUE_MANAGER, OBJECT_LOCAL_QUEUE_MANAGER);
    }
  }

  private void createLocalLockFactoryBeanDefinitions() {
    AtomicBoolean customLockFactoryWasDefined = new AtomicBoolean(false);
    getCustomizationService().getOverriddenService(OBJECT_LOCK_FACTORY).ifPresent(customService -> {
      customLockFactoryWasDefined.set(true);
      getBeanDefinitionRegistry().registerBeanDefinition(OBJECT_LOCK_FACTORY,
                                                         getCustomServiceBeanDefinition(customService, OBJECT_LOCK_FACTORY));
    });

    if (customLockFactoryWasDefined.get()) {
      getBeanDefinitionRegistry()
          .registerBeanDefinition(LOCAL_OBJECT_LOCK_FACTORY, defaultContextServices.get(OBJECT_LOCK_FACTORY));
    } else {
      getBeanDefinitionRegistry().registerAlias(OBJECT_LOCK_FACTORY, LOCAL_OBJECT_LOCK_FACTORY);
    }
  }

  private void createLocalObjectStoreBeanDefinitions() {
    AtomicBoolean anyBaseStoreWasRedefined = new AtomicBoolean(false);
    OBJECT_STORE_NAME_TO_LOCAL_OBJECT_STORE_NAME.entrySet().forEach(objectStoreLocal -> getCustomizationService()
        .getOverriddenService(objectStoreLocal.getKey()).ifPresent(customService -> {
          anyBaseStoreWasRedefined.set(true);
          getBeanDefinitionRegistry().registerBeanDefinition(objectStoreLocal.getKey(),
                                                             getCustomServiceBeanDefinition(customService,
                                                                                            objectStoreLocal
                                                                                                .getKey()));
        }));

    if (anyBaseStoreWasRedefined.get()) {
      final AbstractBeanDefinition beanDefinition = getBeanDefinitionBuilder(MuleObjectStoreManager.class)
          .addPropertyValue("basePersistentStoreKey", OBJECT_LOCAL_STORE_PERSISTENT)
          .addPropertyValue("baseTransientStoreKey", OBJECT_LOCAL_STORE_IN_MEMORY)
          .getBeanDefinition();
      beanDefinition.setPrimary(false);
      getBeanDefinitionRegistry().registerBeanDefinition(LOCAL_OBJECT_STORE_MANAGER, beanDefinition);
    } else {
      getBeanDefinitionRegistry().registerAlias(OBJECT_STORE_MANAGER, LOCAL_OBJECT_STORE_MANAGER);
    }
  }

  private BeanDefinition createMuleContextDefinition() {
    return getBeanDefinitionBuilder(MuleContextFactoryBean.class)
        .addConstructorArgValue(muleContext)
        .getBeanDefinition();
  }

  protected void createBootstrapBeanDefinitions() {
    try {
      SpringRegistryBootstrap springRegistryBootstrap =
          new SpringRegistryBootstrap(artifactType,
                                      muleContext.getRegistryBootstrapServiceDiscoverer(),
                                      this::registerBeanDefinition,
                                      BINDING_PROVIDER_PREDICATE
                                          .or(TRANSFORMER_PREDICATE)
                                          .negate());
      springRegistryBootstrap.initialise();
    } catch (InitialisationException e) {
      throw new RuntimeException(e);
    }
  }

  private static BeanDefinition getPrimaryBeanDefinition(Class<?> beanType) {
    BeanDefinition beanDefinition = getBeanDefinition(beanType);
    beanDefinition.setPrimary(true);

    return beanDefinition;
  }

  private static BeanDefinition getPrimaryBeanDefinition(Object beanType) {
    BeanDefinition beanDefinition = getConstantObjectBeanDefinition(beanType);
    beanDefinition.setPrimary(true);

    return beanDefinition;
  }

  private static BeanDefinition getBeanDefinition(Class<?> beanType, String factoryMethodName) {
    return getBeanDefinitionBuilder(beanType).setFactoryMethod(factoryMethodName).getBeanDefinition();
  }

  protected MuleContextWithRegistry getMuleContext() {
    return muleContext;
  }

  protected Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  protected boolean isAddToolingObjectsToRegistry() {
    return addToolingObjectsToRegistry;
  }

  protected MemoryManagementService getMemoryManagementService() {
    return memoryManagementService;
  }

  private BeanDefinition resolveArtifactIdMeterProvider() {
    if (getBoolean(METRIC_EXPORTER_ENABLED_PROPERTY)) {
      return getBeanDefinitionBuilder(ArtifactMeterProvider.class)
          .addConstructorArgReference(MULE_METER_PROVIDER_KEY)
          .addConstructorArgValue(muleContext.getId()).setPrimary(true).getBeanDefinition();
    }
    return getPrimaryBeanDefinition(MeterProvider.NO_OP);
  }

  private BeanDefinition resolveBaseArtifactMeterProvider() {
    if (getBoolean(METRIC_EXPORTER_ENABLED_PROPERTY)) {
      return getBeanDefinition(DefaultMeterProvider.class);
    }
    return getConstantObjectBeanDefinition(MeterProvider.NO_OP);
  }

  private static BeanDefinition resolveErrorMetricsFactory() {
    if (getBoolean(METRIC_EXPORTER_ENABLED_PROPERTY)) {
      return getBeanDefinition(DefaultErrorMetricsFactory.class);
    }
    return getConstantObjectBeanDefinition(ErrorMetricsFactory.NO_OP);
  }

}
