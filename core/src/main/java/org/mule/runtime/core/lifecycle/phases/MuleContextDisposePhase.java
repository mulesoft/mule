/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle.phases;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.LifecyclePhase;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.lifecycle.LifecycleObject;
import org.mule.runtime.core.lifecycle.NotificationLifecycleObject;
import org.mule.runtime.core.util.annotation.AnnotationMetaData;
import org.mule.runtime.core.util.annotation.AnnotationUtils;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;

/**
 * Objects are disposed of via the Registry since the Registry manages the creation/initialisation of the objects it
 * must also take care of disposing them. However, a user may want to initiate a dispose via the
 * {@link org.mule.runtime.core.DefaultMuleContext} so the dispose Lifecycle phase for the
 * {@link org.mule.runtime.core.DefaultMuleContext} needs to call dispose on the Registry.
 *
 * The MuleContextDisposePhase defines the lifecycle behaviour when the Mule context is disposed. The MuleContext is
 * associated with one or more registries that inherit the lifecycle of the MuleContext.
 *
 * This phase is responsible for disposing objects. Any object that implements
 * {@link org.mule.runtime.core.api.lifecycle.Disposable} will have its
 * {@link org.mule.runtime.core.api.lifecycle.Disposable#dispose()} method called. Objects are initialised in the order
 * based on type: {@link org.mule.runtime.core.api.construct.FlowConstruct},
 * {@link org.mule.runtime.core.api.agent.Agent}, {@link org.mule.runtime.core.api.transport.Connector} followed by any other object
 * that implements {@link org.mule.runtime.core.api.lifecycle.Disposable}.
 *
 * @see org.mule.runtime.core.api.MuleContext
 * @see org.mule.runtime.core.api.lifecycle.LifecycleManager
 * @see org.mule.runtime.core.api.lifecycle.Disposable
 *
 * @since 3.0
 */
public class MuleContextDisposePhase extends DefaultLifecyclePhase
{
    public MuleContextDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);

        Set<LifecycleObject> orderedObjects = new LinkedHashSet<>();
        // Stop in the opposite order to start
        orderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
        orderedObjects.add(new NotificationLifecycleObject(Agent.class));
        orderedObjects.add(new NotificationLifecycleObject(Connector.class));
        orderedObjects.add(new NotificationLifecycleObject(ConfigurationProvider.class));
        orderedObjects.add(new NotificationLifecycleObject(Config.class));
        orderedObjects.add(new NotificationLifecycleObject(Disposable.class));
        orderedObjects.add(new NotificationLifecycleObject(Object.class));

        //Can call dispose from all lifecycle Phases
        registerSupportedPhase(LifecyclePhase.ALL_PHASES);
        setOrderedLifecycleObjects(orderedObjects);
        /*
        Ignored objects -
        -Component is ignored because the FlowConstruct will manage the components lifecycle
        -MessageSource disposal is managed by the connector it is associated with
        -RouterCollection is ignored because the FlowConstruct will manage the lifecycle
        -Router is ignored since its lifecycle is managed by the associated router collection
        -Transformer is ignored since the Dispose lifecycle is managed by the base {@link AbstractTransformer} by receiving
        a CONTEXT_DISPOSING event and calling dispose on the transformer.  This is necessary since transformers are prototype objects
        and not managed by DI containers such as Spring after the creation of the object
         */
        setIgnoredObjectTypes(new Class[]{Component.class, MessageSource.class, OutboundRouter.class, Transformer.class, MuleContext.class});
    }

     @Override
    public void applyLifecycle(Object o) throws LifecycleException
    {
        if (o == null)
        {
            return;
        }
        if (ignoreType(o.getClass()))
        {
            return;
        }
        //retain default Lifecycle behaviour
        try
        {
            super.applyLifecycle(o);
        }
        catch (Exception e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Failed to dispose object " + o, e);
            }
        }

        List<AnnotationMetaData> annos = AnnotationUtils.getMethodAnnotations(o.getClass(), PreDestroy.class);
        if (annos.size() == 0)
        {
            return;
        }
        //Note that the registry has a processor that validates that there is at most one {@link PostConstruct} annotation
        //per object and that the method conforms to a lifecycle method
        AnnotationMetaData anno = annos.get(0);
        try
        {
            ((Method) anno.getMember()).invoke(o);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToInvokeLifecycle(
                    (anno == null ? "null" : anno.getMember().getName()), o), e, this);
        }
    }
}
