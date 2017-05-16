/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.config.spring.MuleArtifactContext.INNER_BEAN_PREFIX;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.el.ExpressionLanguageExtension;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.LegacyConnector;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.lifecycle.LifecycleObject;
import org.mule.runtime.core.lifecycle.NotificationLifecycleObject;
import org.mule.runtime.core.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextDisposePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStartPhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStopPhase;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.registry.AbstractRegistryBroker;
import org.mule.runtime.core.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.util.LinkedHashSet;
import java.util.Set;

public class SpringRegistryLifecycleManager extends RegistryLifecycleManager {

  public SpringRegistryLifecycleManager(String id, SpringRegistry springRegistry, MuleContext muleContext) {
    super(id, springRegistry, muleContext);
  }

  @Override
  protected void registerPhases() {
    final LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();
    registerPhase(NotInLifecyclePhase.PHASE_NAME, new NotInLifecyclePhase(), emptyCallback);
    registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase(), new SpringLifecycleCallback(this));
    registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), emptyCallback);
    registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), emptyCallback);
    registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase());
  }

  // ///////////////////////////////////////////////////////////////////////////////////
  // Spring custom lifecycle phases
  // ///////////////////////////////////////////////////////////////////////////////////

  class SpringContextInitialisePhase extends MuleContextInitialisePhase {

    public SpringContextInitialisePhase() {
      super();

      Set<LifecycleObject> initOrderedObjects = new LinkedHashSet<>();
      initOrderedObjects.add(new NotificationLifecycleObject(ObjectStoreManager.class));
      initOrderedObjects.add(new NotificationLifecycleObject(ExpressionLanguageExtension.class));
      initOrderedObjects.add(new NotificationLifecycleObject(ExtendedExpressionLanguageAdaptor.class));
      initOrderedObjects.add(new NotificationLifecycleObject(QueueManager.class));
      initOrderedObjects.add(new NotificationLifecycleObject(StreamingManager.class));
      initOrderedObjects.add(new NotificationLifecycleObject(ConfigurationProvider.class));
      initOrderedObjects.add(new NotificationLifecycleObject(Config.class));
      initOrderedObjects.add(new NotificationLifecycleObject(LegacyConnector.class));
      initOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
      initOrderedObjects.add(new NotificationLifecycleObject(SecurityManager.class));
      initOrderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
      initOrderedObjects.add(new NotificationLifecycleObject(Initialisable.class));
      setOrderedLifecycleObjects(initOrderedObjects);

      setIgnoredObjectTypes(new Class[] {ExtensionManager.class, SpringRegistry.class, SpringRegistryBootstrap.class,
          Component.class, MessageSource.class, InterceptingMessageProcessor.class, AbstractMessageProcessorOwner.class,
          MessagingExceptionHandler.class, AbstractAsyncRequestReplyRequester.class, OutboundRouter.class,
          MessageProcessorChain.class, MuleContext.class, Service.class});
    }

    @Override
    public void applyLifecycle(Object o) throws LifecycleException {
      if (o instanceof Transformer) {
        String name = ((Transformer) o).getName();
        if (isNamedBean(name)) {
          super.applyLifecycle(o);
        }
      } else {
        super.applyLifecycle(o);
      }
    }
  }

  /**
   * Detects if a bean is an inner bean to prevent applying lifecycle to it since lifecycle is already applied by the owner, i.e.:
   * a flow
   *
   * @param name bean name.
   * @return true if contains inner bean as prefix of the bean name, false otherwise.
   */
  private boolean isNamedBean(String name) {
    return name != null && !name.startsWith(INNER_BEAN_PREFIX);
  }

  /**
   * A lifecycle phase that will delegate to the {@link org.mule.runtime.config.spring.SpringRegistry#doDispose()} method which in
   * turn will destroy the application context managed by this registry
   */
  class SpringContextDisposePhase extends MuleContextDisposePhase {

    public SpringContextDisposePhase() {
      super();
      setIgnoredObjectTypes(new Class[] {Component.class, MessageSource.class, InterceptingMessageProcessor.class,
          OutboundRouter.class, MuleContext.class, ServerNotificationManager.class, Service.class});
    }

    @Override
    public void applyLifecycle(Object o) throws LifecycleException {
      if (o instanceof SpringRegistry) {
        ((SpringRegistry) o).doDispose();
      } else if (o instanceof Transformer) {
        String name = ((Transformer) o).getName();
        if (isNamedBean(name)) {
          super.applyLifecycle(o);
        }
      } else {
        super.applyLifecycle(o);
      }
    }
  }

}
