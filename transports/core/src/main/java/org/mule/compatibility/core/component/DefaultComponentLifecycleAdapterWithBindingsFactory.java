/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.JavaWithBindingsComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.EntryPointResolverSet;

/**
 * <code>DefaultComponentLifecycleAdapterFactory</code> creates a DefaultComponentLifecycleAdapter. Users can implement their own
 * LifeCycleAdapter factories to control lifecycle events on their services such as introduce other lifecycle events that are
 * controlled by external changes.
 *
 * @see org.mule.compatibility.core.api.component.LifecycleAdapter
 * @see org.mule.compatibility.core.api.component.LifecycleAdapterFactory
 * @see org.mule.compatibility.core.component.DefaultComponentLifecycleAdapter
 * @see org.mule.compatibility.core.component.DefaultComponentLifecycleAdapterWithBindingsFactory
 */
public class DefaultComponentLifecycleAdapterWithBindingsFactory implements LifecycleAdapterFactory<JavaWithBindingsComponent> {

  @Override
  public LifecycleAdapter create(Object pojoService, JavaWithBindingsComponent component, FlowConstruct flowConstruct,
                                 EntryPointResolverSet resolver, MuleContext muleContext)
      throws MuleException {
    return new DefaultComponentLifecycleAdapterWithBindings(pojoService, component, flowConstruct, resolver, muleContext);
  }

}
