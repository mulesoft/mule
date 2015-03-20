/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.api.component.Component;
import org.mule.api.config.Config;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.expression.ExpressionEnricher;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.source.MessageSource;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.extension.ExtensionManager;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.NotificationLifecycleObject;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.registry.AbstractRegistryBroker;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;

import java.util.LinkedHashSet;
import java.util.Set;

public class SpringRegistryLifecycleManager extends RegistryLifecycleManager
{

    public SpringRegistryLifecycleManager(String id, SpringRegistry springRegistry, MuleContext muleContext)
    {
        super(id, springRegistry, muleContext);
    }

    protected void registerPhases()
    {
        final LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();
        registerPhase(NotInLifecyclePhase.PHASE_NAME, NOT_IN_LIFECYCLE_PHASE, emptyCallback);
        registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase(), new SpringLifecycleCallback(this));
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), emptyCallback);
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), emptyCallback);
        registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase());
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // Spring custom lifecycle phases
    // ///////////////////////////////////////////////////////////////////////////////////

    class SpringContextInitialisePhase extends MuleContextInitialisePhase
    {

        public SpringContextInitialisePhase()
        {
            super();

            Set<LifecycleObject> initOrderedObjects = new LinkedHashSet<>();
            initOrderedObjects.add(new NotificationLifecycleObject(ExtensionManager.class));
            initOrderedObjects.add(new NotificationLifecycleObject(ObjectStoreManager.class));
            initOrderedObjects.add(new NotificationLifecycleObject(ExpressionEvaluator.class));
            initOrderedObjects.add(new NotificationLifecycleObject(ExpressionEnricher.class));
            initOrderedObjects.add(new NotificationLifecycleObject(ExpressionLanguageExtension.class));
            initOrderedObjects.add(new NotificationLifecycleObject(ExpressionLanguage.class));
            initOrderedObjects.add(new NotificationLifecycleObject(Config.class));
            initOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
            initOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
            initOrderedObjects.add(new NotificationLifecycleObject(Model.class));
            initOrderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
            initOrderedObjects.add(new NotificationLifecycleObject(Initialisable.class));
            setOrderedLifecycleObjects(initOrderedObjects);

            setIgnoredObjectTypes(new Class[] {
                    SpringRegistry.class,
                    SpringRegistryBootstrap.class,
                    Component.class,
                    MessageSource.class,
                    AbstractMessageProcessorOwner.class,
                    MessagingExceptionHandler.class,
                    AbstractAsyncRequestReplyRequester.class,
                    OutboundRouter.class,
                    MessageProcessorChain.class,
                    OutboundRouterCollection.class,
                    MuleContext.class
            });
        }
    }

    /**
     * A lifecycle phase that will delegate to the
     * {@link org.mule.config.spring.SpringRegistry#doDispose()} method which in turn
     * will destroy the application context managed by this registry
     */
    class SpringContextDisposePhase extends MuleContextDisposePhase
    {

        public SpringContextDisposePhase()
        {
            super();
            setIgnoredObjectTypes(new Class[] {
                    Component.class,
                    MessageSource.class,
                    OutboundRouterCollection.class,
                    OutboundRouter.class,
                    Transformer.class,
                    MuleContext.class,
                    ServerNotificationManager.class});
        }

        @Override
        public void applyLifecycle(Object o) throws LifecycleException
        {
            if (o instanceof SpringRegistry)
            {
                ((SpringRegistry) o).doDispose();
            }
            else
            {
                super.applyLifecycle(o);
            }
        }
    }

}
