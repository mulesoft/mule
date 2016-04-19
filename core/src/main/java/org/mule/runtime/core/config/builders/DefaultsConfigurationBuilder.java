/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.builders;

import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.config.ChainedThreadingProfile;
import org.mule.runtime.core.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.runtime.core.connector.MuleConnectorOperationLocator;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguageWrapper;
import org.mule.runtime.core.endpoint.DefaultEndpointFactory;
import org.mule.runtime.core.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.core.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;
import org.mule.runtime.core.security.MuleSecurityManager;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.core.util.DefaultStreamCloserService;
import org.mule.runtime.core.util.lock.MuleLockFactory;
import org.mule.runtime.core.util.lock.SingleServerLockProvider;
import org.mule.runtime.core.util.queue.DelegateQueueManager;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.core.util.store.DefaultObjectStoreFactoryBean;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;

/**
 * Configures defaults required by Mule. This configuration builder is used to configure mule with these defaults when
 * no other ConfigurationBuilder that sets these is being used. This is used by both AbstractMuleTestCase and
 * MuleClient. <br>
 * <br>
 * Default instances of the following are configured:
 * <ul>
 * <li>{@link SimpleRegistryBootstrap}
 * <li>{@link QueueManager}
 * <li>{@link SecurityManager}
 * <li>{@link ObjectStore}
 * <li>{@link DefaultEndpointFactory}
 * <li>{@link ThreadingProfile} defaultThreadingProfile
 * <li>{@link ThreadingProfile} defaultMessageDispatcherThreadingProfile
 * <li>{@link ThreadingProfile} defaultMessageRequesterThreadingProfile
 * <li>{@link ThreadingProfile} defaultMessageReceiverThreadingProfile
 * <li>{@link ThreadingProfile} defaultComponentThreadingProfile
 * </ul>
 */
public class DefaultsConfigurationBuilder extends AbstractConfigurationBuilder
{
    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        MuleRegistry registry = muleContext.getRegistry();

        registry.registerObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP,
            new SimpleRegistryBootstrap());

        configureQueueManager(muleContext);

        registry.registerObject(MuleProperties.OBJECT_MULE_CONTEXT, muleContext);
        registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());

        registry.registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
            DefaultObjectStoreFactoryBean.createDefaultInMemoryObjectStore());

        registry.registerObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME,
            DefaultObjectStoreFactoryBean.createDefaultPersistentObjectStore());

        registerLocalObjectStoreManager(muleContext, registry);

        registry.registerObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME,
                                DefaultObjectStoreFactoryBean.createDefaultInMemoryQueueStore());
        registry.registerObject(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME,
            DefaultObjectStoreFactoryBean.createDefaultPersistentQueueStore());
        registry.registerObject(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME,
            DefaultObjectStoreFactoryBean.createDefaultUserObjectStore());
        registry.registerObject(MuleProperties.DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME,
            DefaultObjectStoreFactoryBean.createDefaultUserTransientObjectStore());
        registry.registerObject(MuleProperties.OBJECT_STORE_MANAGER, new MuleObjectStoreManager());
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER,
            new MuleMessageProcessingManager());

        registry.registerObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY, new DefaultEndpointFactory());
        registry.registerObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE,
            new DefaultStreamCloserService());

        registry.registerObject(MuleProperties.OBJECT_LOCK_PROVIDER, new SingleServerLockProvider());
        registry.registerObject(MuleProperties.OBJECT_LOCK_FACTORY, new MuleLockFactory());

        registry.registerObject(MuleProperties.OBJECT_PROCESSING_TIME_WATCHER,
            new DefaultProcessingTimeWatcher());

        configureThreadingProfiles(registry);

        registry.registerObject(MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE,
                                new NoRetryPolicyTemplate());
        registry.registerObject(MuleProperties.OBJECT_CONVERTER_RESOLVER,
            new DynamicDataTypeConversionResolver(muleContext));

        registry.registerObject(MuleProperties.OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguageWrapper(muleContext));
        registry.registerObject(MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR, new MuleConnectorOperationLocator());
        registry.registerObject(MuleProperties.OBJECT_TIME_SUPPLIER, new TimeSupplier());
        registry.registerObject(MuleProperties.OBJECT_CONNECTION_MANAGER, new DefaultConnectionManager(muleContext));
        registry.registerObject(MuleProperties.OBJECT_METADATA_MANAGER, new MuleMetadataManager());
    }

    private void registerLocalObjectStoreManager(MuleContext muleContext, MuleRegistry registry) throws RegistrationException
    {
        MuleObjectStoreManager osm = new MuleObjectStoreManager();
        osm.setBasePersistentStoreKey(DefaultMuleContext.LOCAL_PERSISTENT_OBJECT_STORE_KEY);
        osm.setBaseTransientStoreKey(DefaultMuleContext.LOCAL_TRANSIENT_OBJECT_STORE_KEY);
        osm.setMuleContext(muleContext);
        registry.registerObject(DefaultMuleContext.LOCAL_PERSISTENT_OBJECT_STORE_KEY, osm);
    }

    protected void configureQueueManager(MuleContext muleContext) throws RegistrationException
    {
        QueueManager queueManager = new DelegateQueueManager();
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
        muleContext.getRegistry().registerObject(DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY, queueManager);
    }

    protected void configureThreadingProfiles(MuleRegistry registry) throws RegistrationException
    {
        ThreadingProfile defaultThreadingProfile = new ChainedThreadingProfile();
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE, defaultThreadingProfile);

        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
    }
}
