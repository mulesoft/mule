
package org.mule.config.builders;

import org.mule.config.AbstractConfigurationBuilder;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.impl.endpoint.EndpointFactory;
import org.mule.impl.model.seda.SedaModel;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.model.UMOModel;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.TransactionalQueueManager;

public class DefaultConfigurationBuilder extends AbstractConfigurationBuilder
{

    protected void doConfigure(UMOManagementContext managementContext, String[] configResources) throws Exception
    {
        configureDefaults(managementContext.getRegistry());
    }

    protected void configureDefaults(Registry registry) throws RegistrationException, UMOException
    {
        registry.registerObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP, new SimpleRegistryBootstrap());
        QueueManager queueManager = new TransactionalQueueManager();
        queueManager.setPersistenceStrategy(new CachingPersistenceStrategy(new MemoryPersistenceStrategy()));
        registry.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
        registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());
        registry.registerObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY, new EndpointFactory());
        ThreadingProfile defaultThreadingProfile = new ChainedThreadingProfile();
        defaultThreadingProfile.setThreadWaitTimeout(30);
        defaultThreadingProfile.setMaxThreadsActive(10);
        defaultThreadingProfile.setMaxThreadsIdle(10);
        defaultThreadingProfile.setMaxBufferSize(0);
        defaultThreadingProfile.setThreadTTL(60000);
        defaultThreadingProfile.setPoolExhaustedAction(4);
        registry.registerObject("defaultThreadingProfile", defaultThreadingProfile);
        registry.registerObject("defaultMessageDispatcherThreadingProfile", new ChainedThreadingProfile(
            defaultThreadingProfile));
        registry.registerObject("defaultMessageRequesterThreadingProfile", new ChainedThreadingProfile(
            defaultThreadingProfile));
        registry.registerObject("defaultMessageReceiverThreadingProfile", new ChainedThreadingProfile(
            defaultThreadingProfile));
        registry.registerObject("defaultComponentThreadingProfile", new ChainedThreadingProfile(
            defaultThreadingProfile));
        UMOModel systemModel = new SedaModel();
        systemModel.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
        registry.registerModel(systemModel);
    }

}
