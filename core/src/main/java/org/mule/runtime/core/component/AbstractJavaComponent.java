/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.model.resolvers.LegacyEntryPointResolverSet;

import java.util.Collection;

/**
 * Abstract implementation of JavaComponent adds JavaComponent specifics like {@link EntryPointResolverSet} and
 * {@link ObjectFactory}.
 */
public abstract class AbstractJavaComponent extends AbstractComponent implements JavaComponent {

  protected EntryPointResolverSet entryPointResolverSet;

  protected ObjectFactory objectFactory;

  protected LifecycleAdapterFactory lifecycleAdapterFactory;

  /**
   * For Spring only
   */
  public AbstractJavaComponent() {
    super();
  }

  public AbstractJavaComponent(ObjectFactory objectFactory) {
    this(objectFactory, null);
  }

  public AbstractJavaComponent(ObjectFactory objectFactory, EntryPointResolverSet entryPointResolverSet) {
    super();
    this.entryPointResolverSet = entryPointResolverSet;
    this.objectFactory = objectFactory;
  }

  @Override
  protected Object doInvoke(MuleEvent event) throws Exception {
    return invokeComponentInstance(event);
  }

  protected Object invokeComponentInstance(MuleEvent event) throws Exception {
    LifecycleAdapter componentLifecycleAdapter = null;
    try {
      componentLifecycleAdapter = borrowComponentLifecycleAdaptor();
      return componentLifecycleAdapter.invoke(event);
    } finally {
      if (componentLifecycleAdapter != null) {
        returnComponentLifecycleAdaptor(componentLifecycleAdapter);
      }
    }
  }

  @Override
  public Class<?> getObjectType() {
    return objectFactory.getObjectClass();
  }

  /**
   * Creates and initialises a new LifecycleAdaptor instance wrapped the component object instance obtained from the configured
   * object factory.
   *
   * @throws MuleException
   * @throws Exception
   */
  protected LifecycleAdapter createLifecycleAdaptor() throws Exception {
    // Todo this could be moved to the LCAFactory potentially
    Object object = objectFactory.getInstance(muleContext);

    LifecycleAdapter lifecycleAdapter;
    if (lifecycleAdapterFactory != null) {
      // Custom lifecycleAdapterFactory set on component
      lifecycleAdapter = lifecycleAdapterFactory.create(object, this, flowConstruct, entryPointResolverSet, muleContext);
    } else if (objectFactory.isExternallyManagedLifecycle()) {
      // If no lifecycleAdapterFactory is configured explicitly and object factory returns
      // externally managed instance then use NullLifecycleAdapter so that lifecycle
      // is not propagated
      lifecycleAdapter = new NullLifecycleAdapter(object, this, flowConstruct, entryPointResolverSet, muleContext);
    } else {
      lifecycleAdapter =
          new DefaultComponentLifecycleAdapterFactory().create(object, this, flowConstruct, entryPointResolverSet, muleContext);
    }
    lifecycleAdapter.initialise();
    return lifecycleAdapter;
  }

  protected abstract LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception;

  protected abstract void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter) throws Exception;

  @Override
  protected void doInitialise() throws InitialisationException {
    if (objectFactory == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("object factory"), this);
    }
    objectFactory.initialise();
  }

  @Override
  protected void doStart() throws MuleException {
    // We need to resolve entry point resolvers here rather than in initialise()
    // because when configuring with spring, although the service has been
    // injected and is available the injected service construction has not been
    // completed and model is still in null.
    if (entryPointResolverSet == null) {
      entryPointResolverSet = new LegacyEntryPointResolverSet();
    }
  }

  @Override
  protected void doDispose() {
    if (objectFactory != null) {
      objectFactory.dispose();
    }
  }

  @Override
  public EntryPointResolverSet getEntryPointResolverSet() {
    return entryPointResolverSet;
  }

  @Override
  public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet) {
    this.entryPointResolverSet = entryPointResolverSet;
  }

  /**
   * Allow for incremental addition of resolvers by for example the spring-config module
   *
   * @param entryPointResolvers Resolvers to add
   */
  public void setEntryPointResolvers(Collection<EntryPointResolver> entryPointResolvers) {
    if (null == entryPointResolverSet) {
      entryPointResolverSet = new DefaultEntryPointResolverSet();
    }

    for (EntryPointResolver resolver : entryPointResolvers) {
      entryPointResolverSet.addEntryPointResolver(resolver);
    }
  }

  @Override
  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  @Override
  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
    injectService();
  }

  @Override
  public LifecycleAdapterFactory getLifecycleAdapterFactory() {
    return lifecycleAdapterFactory;
  }

  @Override
  public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory) {
    this.lifecycleAdapterFactory = lifecycleAdapterFactory;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    super.setFlowConstruct(flowConstruct);
    injectService();
  }

  protected void injectService() {
    if (objectFactory != null && objectFactory instanceof FlowConstructAware && flowConstruct != null) {
      // The registry cannot inject the Service for this object since there is
      // no way to tie the two together, so
      // we set the service on the object factory, that way the factory is
      // responsible for injecting all properties
      // on the result object
      ((FlowConstructAware) objectFactory).setFlowConstruct(flowConstruct);
    }
  }
}
