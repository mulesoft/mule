/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.LifecycleObject;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.privileged.routing.OutboundRouter;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.privileged.transport.LegacyConnector;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Start phase for the MuleContext. Calling {@link MuleContext#start()} will initiate this phase via the
 * {@link org.mule.runtime.core.api.lifecycle.LifecycleManager}.
 * <p/>
 * The MuleContextStartPhase defines the lifecycle behaviour when the Mule context is started. The MuleContext is associated with
 * one or more registries that inherit the lifecycle of the MuleContext.
 * <p/>
 * This phase is responsible for starting objects. Any object that implements {@link Startable} will have its
 * {@link Startable#start()} method called. Objects are initialised in the order based on type:
 * {@link org.mule.runtime.core.api.construct.FlowConstruct}, followed by any other object that implements {@link Startable}.
 *
 * @see org.mule.runtime.core.api.MuleContext
 * @see org.mule.runtime.core.api.lifecycle.LifecycleManager
 * @see Startable
 * @since 3.0
 */
public class MuleContextStartPhase extends DefaultLifecyclePhase {

  public MuleContextStartPhase() {
    this(new Class[] {Registry.class, MuleContext.class, MessageSource.class, InterceptingMessageProcessor.class, Component.class,
        OutboundRouter.class, MuleContext.class, Service.class});
  }

  public MuleContextStartPhase(Class<?>[] ignoredObjects) {
    super(Startable.PHASE_NAME, Startable.class, Stoppable.PHASE_NAME);

    Set<LifecycleObject> startOrderedObjects = new LinkedHashSet<>();
    startOrderedObjects.add(new LifecycleObject(QueueManager.class));
    startOrderedObjects.add(new LifecycleObject(ConfigurationProvider.class));
    startOrderedObjects.add(new LifecycleObject(Config.class));
    startOrderedObjects.add(new LifecycleObject(LegacyConnector.class));
    startOrderedObjects.add(new LifecycleObject(FlowConstruct.class));
    startOrderedObjects.add(new LifecycleObject(Startable.class));

    setIgnoredObjectTypes(ignoredObjects);
    setOrderedLifecycleObjects(startOrderedObjects);
    registerSupportedPhase(Initialisable.PHASE_NAME);
    // Start/Stop/Start
    registerSupportedPhase(Stoppable.PHASE_NAME);
  }
}
