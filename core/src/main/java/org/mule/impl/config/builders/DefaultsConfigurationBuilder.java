
package org.mule.impl.config.builders;

import org.mule.api.MuleContext;
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
import org.mule.umo.model.UMOModel;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.TransactionalQueueManager;

/**
 * Configures defaults required by Mule. This configuration builder is used to
 * configure mule with these defaults when no other ConfigurationBuilder that sets
 * these is being used. This is used by both AbstractMuleTestCase and MuleClient.
 * <br>
 * <br>
 * Default instances of the following are configured:
 * <li> {@link SimpleRegistryBootstrap}
 * <li> {@link QueueManager}
 * <li> {@link SecurityManager}
 * <li> {@link EndpointFactory}
 * <li> {@link UMOModel} systemModel
 * <li> {@link ThreadingProfile} defaultThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageDispatcherThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageRequesterThreadingProfile
 * <li> {@link ThreadingProfile} defaultMessageReceiverThreadingProfile
 * <li> {@link ThreadingProfile} defaultComponentThreadingProfile
 */
public class DefaultsConfigurationBuilder extends AbstractConfigurationBuilder
{

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        configureDefaults(muleContext.getRegistry());
    }

    protected void configureDefaults(Registry registry) throws RegistrationException, UMOException
    {
        registry.registerObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP,
            new SimpleRegistryBootstrap());
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
        defaultThreadingProfile.setPoolExhaustedAction(ThreadingProfile.WHEN_EXHAUSTED_RUN);
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE, defaultThreadingProfile);
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        registry.registerObject(MuleProperties.OBJECT_DEFAULT_COMPONENT_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
        UMOModel systemModel = new SedaModel();
        systemModel.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
        registry.registerModel(systemModel);
    }
}
