/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.builders;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.core.DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultInMemoryObjectStore;
import static org.mule.runtime.core.internal.util.store.DefaultObjectStoreFactoryBean.createDefaultPersistentObjectStore;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.time.TimeSupplier;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.el.DefaultExpressionManager;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.connector.MuleConnectorOperationLocator;
import org.mule.runtime.core.internal.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.internal.util.DefaultStreamCloserService;
import org.mule.runtime.core.internal.util.queue.TransactionalQueueManager;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;

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
    MuleRegistry registry = muleContext.getRegistry();

    new SimpleRegistryBootstrap(APP, muleContext).initialise();

    configureQueueManager(muleContext);

    registry.registerObject(OBJECT_MULE_CONTEXT, muleContext);
    registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager(), muleContext);

    registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY, createDefaultInMemoryObjectStore(), muleContext);
    registerObject(BASE_PERSISTENT_OBJECT_STORE_KEY, createDefaultPersistentObjectStore(), muleContext);

    registerLocalObjectStoreManager(muleContext, registry);

    registerObject(OBJECT_STORE_MANAGER, new MuleObjectStoreManager(), muleContext);
    registerObject(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, new MuleMessageProcessingManager(), muleContext);

    registerObject(OBJECT_MULE_STREAM_CLOSER_SERVICE, new DefaultStreamCloserService(), muleContext);

    registerObject(OBJECT_LOCK_PROVIDER, new SingleServerLockProvider(), muleContext);
    registerObject(OBJECT_LOCK_FACTORY, new MuleLockFactory(), muleContext);

    registerObject(OBJECT_PROCESSING_TIME_WATCHER, new DefaultProcessingTimeWatcher(), muleContext);

    registerObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, new NoRetryPolicyTemplate(), muleContext);
    registerObject(OBJECT_CONVERTER_RESOLVER, new DynamicDataTypeConversionResolver(muleContext), muleContext);

    registerObject(DEFAULT_OBJECT_SERIALIZER_NAME, new JavaObjectSerializer(), muleContext);
    registerObject(OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguage(muleContext), muleContext);
    StreamingManager streamingManager = new DefaultStreamingManager();
    registerObject(OBJECT_STREAMING_MANAGER, streamingManager, muleContext);
    registerObject(OBJECT_EXPRESSION_MANAGER, new DefaultExpressionManager(muleContext, streamingManager), muleContext);
    registerObject(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR, new MuleConnectorOperationLocator(), muleContext);
    registerObject(OBJECT_TIME_SUPPLIER, new TimeSupplier(), muleContext);
    registerObject(OBJECT_CONNECTION_MANAGER, new DefaultConnectionManager(muleContext), muleContext);
    registerObject(OBJECT_METADATA_SERVICE, new MuleMetadataService(), muleContext);
    registerObject(OBJECT_VALUE_PROVIDER_SERVICE, new MuleValueProviderService(), muleContext);
    registerObject(OBJECT_SCHEDULER_POOLS_CONFIG, SchedulerContainerPoolsConfig.getInstance(), muleContext);
    registerObject(OBJECT_SCHEDULER_BASE_CONFIG,
                   config().withPrefix(muleContext.getConfiguration().getId())
                       .withShutdownTimeout(() -> muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS),
                   muleContext);
  }

  protected void registerObject(String serviceId, Object serviceImpl, MuleContext muleContext) throws RegistrationException {
    if (serviceImpl instanceof MuleContextAware) {
      ((MuleContextAware) serviceImpl).setMuleContext(muleContext);
    }
    muleContext.getRegistry().registerObject(serviceId, serviceImpl);
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
