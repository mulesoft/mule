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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.component.DefaultComponentLifecycleAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class DefaultComponentLifecycleAdapterWithBindings extends DefaultComponentLifecycleAdapter {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(DefaultComponentLifecycleAdapterWithBindings.class);

  public DefaultComponentLifecycleAdapterWithBindings(Object componentObject, JavaWithBindingsComponent component,
                                                      FlowConstruct flowConstruct, EntryPointResolverSet entryPointResolver,
                                                      MuleContext muleContext)
      throws MuleException {

    super(componentObject, component, flowConstruct, muleContext);
    BindingUtils.configureBinding(component, componentObject);
  }
}
