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
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.source.MessageSource;
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

import javax.annotation.PostConstruct;

/**
 * The MuleContextInitialisePhase defines the lifecycle behaviour when the Mule context is initialised.  The MuleContext is associated
 * with one or more registries that inherit the lifecycle of the MuleContext.
 * <p/>
 * This phase is responsible for initialising objects. Any object that implements {@link org.mule.runtime.core.api.lifecycle.Initialisable} will
 * have its {@link org.mule.runtime.core.api.lifecycle.Initialisable#initialise()} method called.  Objects are initialised in the order based on type:
 * {@link org.mule.runtime.core.api.agent.Agent}, {@link org.mule.runtime.core.api.construct.FlowConstruct}, followed
 * by any other object that implements {@link org.mule.runtime.core.api.lifecycle.Initialisable}.
 *
 * @see org.mule.runtime.core.api.MuleContext
 * @see org.mule.runtime.core.api.lifecycle.LifecycleManager
 * @see org.mule.runtime.core.api.lifecycle.Initialisable
 * @since 3.0
 */
public class MuleContextInitialisePhase extends DefaultLifecyclePhase
{
    public MuleContextInitialisePhase()
    {
        super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);
        registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);

        Set<LifecycleObject> orderedObjects = new LinkedHashSet<>();
        orderedObjects.add(new NotificationLifecycleObject(ConfigurationProvider.class));
        orderedObjects.add(new NotificationLifecycleObject(Config.class));
        orderedObjects.add(new NotificationLifecycleObject(Connector.class));
        orderedObjects.add(new NotificationLifecycleObject(Agent.class));
        orderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
        orderedObjects.add(new NotificationLifecycleObject(Initialisable.class));
        setOrderedLifecycleObjects(orderedObjects);
        setIgnoredObjectTypes(new Class[]{Component.class, MessageSource.class, OutboundRouter.class, MuleContext.class});
    }


    @Override
    public void applyLifecycle(Object o) throws LifecycleException
    {
        //retain default Lifecycle behaviour
        super.applyLifecycle(o);
        if (o == null)
        {
            return;
        }
        if (ignoreType(o.getClass()))
        {
            return;
        }

        //Lets check for {@link PostConstruct} annotations on methods of this object and invoke
        List<AnnotationMetaData> annos = AnnotationUtils.getMethodAnnotations(o.getClass(), PostConstruct.class);

        //Note that the registry has a processor that validates that there is at most one {@link PostConstruct} annotation
        //per object and that the method conforms to a lifecycle method
        if (annos.size() == 1)
        {
            AnnotationMetaData anno = annos.get(0);

            try
            {
                ((Method) anno.getMember()).invoke(o);
            }
            catch (Exception e)
            {
                throw new LifecycleException(CoreMessages.failedToInvokeLifecycle(anno.getMember().getName(), o), e, this);
            }
        }

    }
}
