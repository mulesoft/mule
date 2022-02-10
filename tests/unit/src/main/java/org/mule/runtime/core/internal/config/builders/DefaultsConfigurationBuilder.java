/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.builders;

import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PAYLOAD_STATISTICS_DECORATOR_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
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
import static org.mule.runtime.core.internal.context.DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.internal.interception.InterceptorManager.INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultInMemoryObjectStore;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultPersistentObjectStore;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.cluster.DefaultClusterService;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.connection.DefaultConnectivityTesterFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.event.DefaultEventContextService;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.internal.management.stats.NoOpCursorDecoratorFactory;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.processor.interceptor.DefaultProcessorInterceptorManager;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.internal.time.LocalTimeSupplier;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.util.DefaultResourceLocator;
import org.mule.runtime.core.internal.util.DefaultStreamCloserService;
import org.mule.runtime.core.internal.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;

import java.util.Map.Entry;

/**
 * Configures defaults required by Mule. This configuration builder is used to configure mule with these defaults when no other
 * ConfigurationBuilder that sets these is being used. This is used by both AbstractMuleTestCase and MuleClient. <br>
 * <br>
 * Default instances of the following are configured:
 * <ul>
 * <li>{@link SimpleRegistryBootstrap}
 * <li>{@link QueueManager}
 * <li>{@link SecurityManager}
 * <li>{@link ObjectStore}
 * </ul>
 */
public class DefaultsConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();

    new SimpleRegistryBootstrap(APP, muleContext).initialise();

    configureQueueManager(muleContext);

    registry.registerObject(OBJECT_MULE_CONTEXT, muleContext);

    for (Entry<String, CustomService> entry : ((CustomServiceRegistry) (muleContext.getCustomizationService()))
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

    registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager(), muleContext);

    registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY, createDefaultInMemoryObjectStore(), muleContext);
    registerObject(BASE_PERSISTENT_OBJECT_STORE_KEY, createDefaultPersistentObjectStore(), muleContext);

    registerLocalObjectStoreManager(muleContext, registry);

    registerObject(OBJECT_SCHEDULER_POOLS_CONFIG, SchedulerContainerPoolsConfig.getInstance(), muleContext);
    registerObject(OBJECT_SCHEDULER_BASE_CONFIG,
                   config().withPrefix(muleContext.getConfiguration().getId())
                       .withShutdownTimeout(() -> muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS),
                   muleContext);

    registerObject(OBJECT_STORE_MANAGER, new MuleObjectStoreManager(), muleContext);
    registerObject(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, new MuleMessageProcessingManager(), muleContext);

    registerObject(OBJECT_MULE_STREAM_CLOSER_SERVICE, new DefaultStreamCloserService(), muleContext);

    registerObject(OBJECT_LOCK_PROVIDER, new SingleServerLockProvider(), muleContext);
    registerObject(OBJECT_LOCK_FACTORY, new MuleLockFactory(), muleContext);

    registerObject(OBJECT_PROCESSING_TIME_WATCHER, new DefaultProcessingTimeWatcher(), muleContext);
    registerObject(OBJECT_PAYLOAD_STATISTICS_DECORATOR_FACTORY, new NoOpCursorDecoratorFactory(), muleContext);

    TransformersRegistry transformersRegistry = new DefaultTransformersRegistry();
    registerObject(OBJECT_TRANSFORMERS_REGISTRY, transformersRegistry, muleContext);
    registerObject(OBJECT_CONVERTER_RESOLVER, new DynamicDataTypeConversionResolver(transformersRegistry), muleContext);
    registerObject(OBJECT_TRANSFORMATION_SERVICE, new ExtendedTransformationService(muleContext), muleContext);
    registerObject(OBJECT_TRANSFORMER_RESOLVER, new TypeBasedTransformerResolver(), muleContext);

    registerObject(DEFAULT_OBJECT_SERIALIZER_NAME, new JavaObjectSerializer(), muleContext);

    final ContributedErrorTypeRepository contributedErrorTypeRepository = new ContributedErrorTypeRepository();
    registerObject(ErrorTypeRepository.class.getName(), contributedErrorTypeRepository, muleContext);
    final ContributedErrorTypeLocator contributedErrorTypeLocator = new ContributedErrorTypeLocator();
    contributedErrorTypeLocator.setDelegate(createDefaultErrorTypeLocator(contributedErrorTypeRepository));
    registerObject(ErrorTypeLocator.class.getName(), contributedErrorTypeLocator, muleContext);

    try {
      Class<?> mvelLangCls = Class.forName("org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage");
      mvelLangCls.getConstructor(MuleContext.class).newInstance(muleContext);
      registerObject(OBJECT_EXPRESSION_LANGUAGE, mvelLangCls.getConstructor(MuleContext.class).newInstance(muleContext),
                     muleContext);
      registerObject(COMPATIBILITY_PLUGIN_INSTALLED, true, muleContext);
    } catch (ClassNotFoundException cnfe) {
      // no mvel in classpath, move on
    }

    registerObject(OBJECT_STREAMING_GHOST_BUSTER, new StreamingGhostBuster(), muleContext);
    registerObject(OBJECT_STREAMING_MANAGER, new DefaultStreamingManager(), muleContext);
    DefaultExpressionManager expressionManager = new DefaultExpressionManager();
    DefaultExpressionLanguageFactoryService service = registry.lookupObject(DefaultExpressionLanguageFactoryService.class);
    expressionManager.setExpressionLanguage(new DataWeaveExpressionLanguageAdaptor(muleContext, null, service, null));
    registerObject(OBJECT_EXPRESSION_MANAGER, expressionManager, muleContext);
    registerObject(OBJECT_TIME_SUPPLIER, new LocalTimeSupplier(), muleContext);
    registerObject(OBJECT_CONNECTION_MANAGER, new DefaultConnectionManager(muleContext), muleContext);
    registerObject(METADATA_SERVICE_KEY, new MuleMetadataService(), muleContext);
    registerObject(VALUE_PROVIDER_SERVICE_KEY, new MuleValueProviderService(), muleContext);
    registerObject(INTERCEPTOR_MANAGER_REGISTRY_KEY, new DefaultProcessorInterceptorManager(), muleContext);
    registerObject(OBJECT_NOTIFICATION_DISPATCHER, new DefaultNotificationDispatcher(), muleContext);
    registerObject(NotificationListenerRegistry.REGISTRY_KEY, new DefaultNotificationListenerRegistry(), muleContext);
    registerObject(EventContextService.REGISTRY_KEY, new DefaultEventContextService(), muleContext);
    registerObject(OBJECT_TRANSACTION_FACTORY_LOCATOR, new TransactionFactoryLocator(), muleContext);
    registerObject(OBJECT_CLUSTER_SERVICE, new DefaultClusterService(), muleContext);
    registerObject(OBJECT_CONNECTIVITY_TESTER_FACTORY, new DefaultConnectivityTesterFactory(), muleContext);

    // This is overridden only if no other test configurator has set the profiling service.
    if (((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(MULE_PROFILING_SERVICE_KEY) == null) {
      registerObject(MULE_PROFILING_SERVICE_KEY, new DefaultProfilingService(), muleContext);
    }

    registerObject(ComponentInitialStateManager.SERVICE_ID, new ComponentInitialStateManager() {

      @Override
      public boolean mustStartMessageSource(Component component) {
        return true;
      }
    }, muleContext);
    registerObject(OBJECT_RESOURCE_LOCATOR, new DefaultResourceLocator(), muleContext);
  }

  protected void registerObject(String serviceId, Object serviceImpl, MuleContext muleContext) throws RegistrationException {
    if (serviceImpl instanceof MuleContextAware) {
      ((MuleContextAware) serviceImpl).setMuleContext(muleContext);
    }
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(serviceId, serviceImpl);
  }

  private void registerLocalObjectStoreManager(MuleContext muleContext, MuleRegistry registry) throws RegistrationException {
    MuleObjectStoreManager osm = new MuleObjectStoreManager();
    osm.setBasePersistentStoreKey(BASE_PERSISTENT_OBJECT_STORE_KEY);
    osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
    osm.setMuleContext(muleContext);
    registry.registerObject(OBJECT_STORE_MANAGER, osm);
  }

  protected void configureQueueManager(MuleContext muleContext) throws RegistrationException {
    QueueManager queueManager = new TransactionalQueueManager();
    registerObject(OBJECT_QUEUE_MANAGER, queueManager, muleContext);
    registerObject(LOCAL_QUEUE_MANAGER_KEY, queueManager, muleContext);
  }
}
