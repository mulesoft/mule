/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.builders;

import org.mule.DynamicDataTypeConversionResolver;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.model.Model;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStore;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.management.stats.DefaultProcessingTimeWatcher;
import org.mule.model.seda.SedaModel;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.security.MuleSecurityManager;
import org.mule.util.DefaultStreamCloserService;
import org.mule.util.lock.MuleLockManager;
import org.mule.util.lock.SingleServerLockProvider;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.store.DefaultObjectStoreFactoryBean;
import org.mule.util.store.MuleObjectStoreManager;

/**
 * Configures defaults required by Mule. This configuration builder is used to
 * configure mule with these defaults when no other ConfigurationBuilder that sets
 * these is being used. This is used by both AbstractMuleTestCase and MuleClient.
 * <br>
 * <br>
 * Default instances of the following are configured:
 * <ul>
 * <li> {@link SimpleRegistryBootstrap}
 * <li> {@link QueueManager}
 * <li> {@link SecurityManager}
 * <li> {@link ObjectStore}
 * <li> {@link DefaultEndpointFactory}
 * <li> {@link Model} systemModel
 * <li> {@link ThreadingProfile} defaultThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageDispatcherThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageRequesterThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageReceiverThreadingProfile
 * <li> {@link ThreadingProfile} defaultComponentThreadingProfile
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

        registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());

        registry.registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, DefaultObjectStoreFactoryBean.createDefaultInMemoryObjectStore());
        registry.registerObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME, DefaultObjectStoreFactoryBean.createDefaultPersistentObjectStore());
        registry.registerObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME, DefaultObjectStoreFactoryBean.createDefaultInMemoryQueueStore());
        registry.registerObject(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME, DefaultObjectStoreFactoryBean.createDefaultPersistentQueueStore());
        registry.registerObject(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME, DefaultObjectStoreFactoryBean.createDefaultUserObjectStore());
        registry.registerObject(MuleProperties.OBJECT_STORE_MANAGER, new MuleObjectStoreManager());

        registry.registerObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY, new DefaultEndpointFactory());
        registry.registerObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE, new DefaultStreamCloserService());

        registry.registerObject(MuleProperties.OBJECT_LOCK_MANAGER, new MuleLockManager());
        registry.registerObject(MuleProperties.OBJECT_LOCK_PROVIDER, new SingleServerLockProvider());

        registry.registerObject(MuleProperties.OBJECT_PROCESSING_TIME_WATCHER, new DefaultProcessingTimeWatcher());
        
        configureThreadingProfiles(registry);

        registry.registerObject(MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, new NoRetryPolicyTemplate());
        registry.registerObject(MuleProperties.OBJECT_CONVERTER_RESOLVER, new DynamicDataTypeConversionResolver(muleContext));

        configureSystemModel(registry);
        
        registry.registerObject(MuleProperties.OBJECT_EXPRESSION_LANGUAGE, new MVELExpressionLanguage(muleContext));
    }

    protected void configureQueueManager(MuleContext muleContext) throws RegistrationException
    {
        QueueManager queueManager = new TransactionalQueueManager();
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
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

    protected void configureSystemModel(MuleRegistry registry) throws MuleException
    {
        Model systemModel = new SedaModel();
        systemModel.setName(MuleProperties.OBJECT_SYSTEM_MODEL);

        registry.registerModel(systemModel);
    }
}
