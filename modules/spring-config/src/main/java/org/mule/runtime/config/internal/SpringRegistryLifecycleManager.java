/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.config.internal.MuleArtifactContext.INNER_BEAN_PREFIX;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageExtension;
import org.mule.runtime.core.internal.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextDisposePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStartPhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStopPhase;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.AbstractRegistryBroker;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.internal.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.OutboundRouter;
import org.mule.runtime.core.privileged.transport.LegacyConnector;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpringRegistryLifecycleManager extends RegistryLifecycleManager {

  public SpringRegistryLifecycleManager(String id, SpringRegistry springRegistry, MuleContext muleContext,
                                        LifecycleInterceptor lifecycleInterceptor) {
    super(id, springRegistry, muleContext, lifecycleInterceptor);
  }

  @Override
  protected void registerPhases(Registry registry) {
    final LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();
    registerPhase(NotInLifecyclePhase.PHASE_NAME, new NotInLifecyclePhase(), emptyCallback);
    registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase(registry),
                  new SpringLifecycleCallback(this, (SpringRegistry) registry));
    registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(registry), emptyCallback);
    registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(registry), emptyCallback);
    registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase(registry));
  }

  // ///////////////////////////////////////////////////////////////////////////////////
  // Spring custom lifecycle phases
  // ///////////////////////////////////////////////////////////////////////////////////


  interface SpringLifecyclePhase extends LifecyclePhase {

    Class<?>[] getOrderedLifecycleTypes();
  }


  class SpringContextInitialisePhase extends MuleContextInitialisePhase implements SpringLifecyclePhase {

    public SpringContextInitialisePhase(Registry registry) {
      super(registry);

      setOrderedLifecycleTypes(new Class<?>[] {
          LockFactory.class,
          ObjectStoreManager.class,
          ExpressionLanguageExtension.class,
          QueueManager.class,
          StreamingManager.class,
          ConfigurationProvider.class,
          Config.class,
          LegacyConnector.class,
          SecurityManager.class,
          FlowConstruct.class,
          Initialisable.class
      });

      setIgnoredObjectTypes(new Class[] {ExtensionManager.class, SpringRegistry.class, SpringRegistryBootstrap.class,
          Component.class, MessageSource.class, InterceptingMessageProcessor.class, AbstractMessageProcessorOwner.class,
          FlowExceptionHandler.class, AbstractAsyncRequestReplyRequester.class, OutboundRouter.class,
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

    @Override
    public Class<?>[] getOrderedLifecycleTypes() {
      return orderedLifecycleTypes;
    }

    @Override
    public List<Object> getLifecycleObjects() {
      return SpringRegistryLifecycleManager.this.getLifecycleObjects(this);
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
   * A lifecycle phase that will delegate to the {@link SpringRegistry#doDispose()} method which in turn will destroy the
   * application context managed by this registry
   */
  class SpringContextDisposePhase extends MuleContextDisposePhase implements SpringLifecyclePhase {

    public SpringContextDisposePhase(Registry registry) {
      super(registry);
      setIgnoredObjectTypes(new Class[] {Component.class, MessageSource.class, InterceptingMessageProcessor.class,
          OutboundRouter.class, MuleContext.class, ServerNotificationManager.class, Service.class});
    }

    @Override
    public List<Object> getLifecycleObjects() {
      return SpringRegistryLifecycleManager.this.getLifecycleObjects(this);
    }

    @Override
    public Class<?>[] getOrderedLifecycleTypes() {
      return orderedLifecycleTypes;
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

  private List<Object> getLifecycleObjects(SpringLifecyclePhase phase) {
    final SpringRegistry springRegistry = (SpringRegistry) getLifecycleObject();

    Map<String, Object> objects = springRegistry.lookupEntriesForLifecycle((Class<Object>) phase.getLifecycleClass());
    Class<?>[] orderedLifecycleTypes = phase.getOrderedLifecycleTypes();
    List<Object>[] buckets = new List[orderedLifecycleTypes.length];

    int objectCount = 0;

    for (Map.Entry<String, Object> entry : objects.entrySet()) {
      for (int i = 0; i < phase.getOrderedLifecycleTypes().length; i++) {
        Object object = entry.getValue();
        if (orderedLifecycleTypes[i].isInstance(object)) {
          List<Object> bucket = buckets[i];
          if (bucket == null) {
            bucket = new LinkedList<>();
            buckets[i] = bucket;
          }
          final List<Object> dependencies = springRegistry.getBeanDependencyResolver().resolveBeanDependencies(entry.getKey());
          bucket.addAll(dependencies);
          bucket.add(object);
          objectCount += dependencies.size() + 1;
          break;
        }
      }
    }

    List<Object> sorted = new ArrayList<>(objectCount);
    for (List<Object> bucket : buckets) {
      if (bucket != null) {
        sorted.addAll(bucket);
      }
    }
    return sorted;
  }

}
