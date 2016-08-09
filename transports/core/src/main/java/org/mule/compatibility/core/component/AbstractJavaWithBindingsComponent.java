/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.api.component.JavaWithBindingsComponent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.component.AbstractJavaComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of JavaComponent adds JavaComponent specifics like {@link InterfaceBinding}.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public abstract class AbstractJavaWithBindingsComponent extends AbstractJavaComponent implements JavaWithBindingsComponent {

  protected List<InterfaceBinding> bindings = new ArrayList<InterfaceBinding>();

  /**
   * For Spring only
   */
  public AbstractJavaWithBindingsComponent() {
    super();
  }

  public AbstractJavaWithBindingsComponent(ObjectFactory objectFactory) {
    this(objectFactory, null, null);
  }

  public AbstractJavaWithBindingsComponent(ObjectFactory objectFactory, EntryPointResolverSet entryPointResolverSet,
                                           List<InterfaceBinding> bindings) {
    super(objectFactory, entryPointResolverSet);
    if (bindings != null) {
      this.bindings = bindings;
    }
  }

  @Override
  public List<InterfaceBinding> getInterfaceBindings() {
    return bindings;
  }

  @Override
  public void setInterfaceBindings(List<InterfaceBinding> bindings) {
    this.bindings = bindings;
  }

  /**
   * Creates and initialises a new LifecycleAdaptor instance wrapped the component object instance obtained from the configured
   * object factory.
   *
   * @throws MuleException
   * @throws Exception
   */
  @Override
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
      lifecycleAdapter = new NullLifecycleAdapterWithBindings(object, this, flowConstruct, entryPointResolverSet, muleContext);
    } else {
      lifecycleAdapter = new DefaultComponentLifecycleAdapterWithBindingsFactory().create(object, this, flowConstruct,
                                                                                          entryPointResolverSet, muleContext);
    }
    lifecycleAdapter.initialise();
    return lifecycleAdapter;
  }

}
