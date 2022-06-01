/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_USE_LEGACY_LIFECYCLE_OBJECT_SORTER;
import static org.mule.runtime.api.util.MuleSystemProperties.REUSE_GLOBAL_ERROR_HANDLER_PROPERTY;
import static org.mule.runtime.config.internal.context.MuleArtifactContext.INNER_BEAN_PREFIX;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

import org.apache.commons.lang3.ArrayUtils;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.config.internal.DependencyGraphLifecycleObjectSorter;
import org.mule.runtime.config.internal.resolvers.AutoDiscoveredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DeclaredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageExtension;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.internal.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextDisposePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStartPhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStopPhase;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.OutboundRouter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.Map;

public class SpringRegistryLifecycleManager extends RegistryLifecycleManager {

  private static final boolean USE_LEGACY_SORTER = parseBoolean(getProperty(MULE_USE_LEGACY_LIFECYCLE_OBJECT_SORTER, "false"));

  public SpringRegistryLifecycleManager(String id, Registry springRegistry, MuleContext muleContext,
                                        LifecycleInterceptor lifecycleInterceptor) {
    super(id, springRegistry, muleContext, lifecycleInterceptor);
  }

  @Override
  protected void registerPhases(Registry registry) {
    final RegistryLifecycleCallback<Registry> callback = new RegistryLifecycleCallback<>(this);

    registerPhase(NotInLifecyclePhase.PHASE_NAME, new NotInLifecyclePhase(), new EmptyLifecycleCallback<>());
    registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase(), callback);
    registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), callback);
    registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), callback);
    registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase(), callback);
  }

  @Override
  protected Map<String, Object> lookupObjectsForLifecycle() {
    if (Initialisable.PHASE_NAME.equals(getExecutingPhase())) {
      // Initialise has already been done for the objects in the base registry
      return getSpringRegistry().lookupEntriesForLifecycle(Object.class);
    } else {
      return getSpringRegistry().lookupEntriesForLifecycleIncludingAncestors(Object.class);
    }
  }

  // ///////////////////////////////////////////////////////////////////////////////////
  // Spring custom lifecycle phases
  // ///////////////////////////////////////////////////////////////////////////////////


  class SpringContextInitialisePhase extends MuleContextInitialisePhase {

    public SpringContextInitialisePhase() {
      super();

      setOrderedLifecycleTypes(new Class<?>[] {
          LockFactory.class,
          ObjectStoreManager.class,
          ExpressionLanguageExtension.class,
          ExpressionLanguage.class,
          QueueManager.class,
          StreamingManager.class,
          ConfigurationProvider.class,
          Config.class,
          SecurityManager.class,
          FlowConstruct.class,
          MuleConfiguration.class,
          Initialisable.class
      });

      setIgnoredObjectTypes(new Class[] {
          ExtensionManager.class,
          AbstractSpringRegistry.class,
          SpringRegistryBootstrap.class,
          Component.class,
          InterceptingMessageProcessor.class,
          FlowExceptionHandler.class,
          OutboundRouter.class,
          MessageProcessorChain.class,
          MuleContext.class,
          Service.class
      });
    }


    @Override
    public void applyLifecycle(Object o) throws LifecycleException {
      try {
        o = muleContext.getInjector().inject(o);
      } catch (MuleException e) {
        throw new LifecycleException(e, o);
      }
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
    public LifecycleObjectSorter newLifecycleObjectSorter() {
      if (USE_LEGACY_SORTER) {
        return new SpringLifecycleObjectSorter(orderedLifecycleTypes, getSpringRegistry());
      }
      AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver =
          new AutoDiscoveredDependencyResolver(getSpringRegistry());
      DeclaredDependencyResolver declaredDependencyResolver = new DeclaredDependencyResolver(getSpringRegistry());
      ConfigurationDependencyResolver configurationDependencyResolver =
          getSpringRegistry().getConfigurationDependencyResolver();
      DependencyGraphBeanDependencyResolver dependencyGraphBeanDependencyResolver =
          new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                                    autoDiscoveredDependencyResolver, getSpringRegistry());
      return new DependencyGraphLifecycleObjectSorter(dependencyGraphBeanDependencyResolver, orderedLifecycleTypes);
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
   * A lifecycle phase that will delegate to the {@link AbstractSpringRegistry#doDispose()} method which in turn will destroy the
   * application context managed by this registry
   */
  class SpringContextDisposePhase extends MuleContextDisposePhase {

    public SpringContextDisposePhase() {
      super();
      Class<?>[] ignoredObjects = new Class[] {
          Component.class,
          InterceptingMessageProcessor.class,
          OutboundRouter.class,
          MuleContext.class,
          ServerNotificationManager.class,
          Service.class
      };
      if (parseBoolean(getProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY))) {
        ignoredObjects = ArrayUtils.add(ignoredObjects, GlobalErrorHandler.class);
      }
      setIgnoredObjectTypes(ignoredObjects);
    }

    @Override
    public void applyLifecycle(Object o) throws LifecycleException {
      if (o instanceof AbstractSpringRegistry) {
        ((AbstractSpringRegistry) o).doDispose();
      } else if (o instanceof Transformer) {
        String name = ((Transformer) o).getName();
        if (isNamedBean(name)) {
          super.applyLifecycle(o);
        }
      } else {
        super.applyLifecycle(o);
      }
    }

    @Override
    public LifecycleObjectSorter newLifecycleObjectSorter() {
      if (USE_LEGACY_SORTER) {
        return new SpringLifecycleObjectSorter(orderedLifecycleTypes, getSpringRegistry());
      }
      AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver =
          new AutoDiscoveredDependencyResolver(getSpringRegistry());
      DeclaredDependencyResolver declaredDependencyResolver = new DeclaredDependencyResolver(getSpringRegistry());
      ConfigurationDependencyResolver configurationDependencyResolver =
          getSpringRegistry().getConfigurationDependencyResolver();
      DependencyGraphBeanDependencyResolver dependencyGraphBeanDependencyResolver =
          new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                                    autoDiscoveredDependencyResolver, getSpringRegistry());
      return new DependencyGraphLifecycleObjectSorter(dependencyGraphBeanDependencyResolver, orderedLifecycleTypes);
    }
  }

  private AbstractSpringRegistry getSpringRegistry() {
    return (AbstractSpringRegistry) getLifecycleObject();
  }
}
