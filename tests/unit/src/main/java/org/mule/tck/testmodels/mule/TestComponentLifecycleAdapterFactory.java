/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.component.LifecycleAdapter;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.EntryPointResolverSet;

public class TestComponentLifecycleAdapterFactory implements LifecycleAdapterFactory<JavaComponent> {

  public TestComponentLifecycleAdapterFactory() {
    super();
  }

  @Override
  public LifecycleAdapter create(Object pojoService, JavaComponent service, FlowConstruct flowConstruct,
                                 EntryPointResolverSet resolver, MuleContext muleContext)
      throws MuleException {
    return new TestComponentLifecycleAdapter(pojoService, service, flowConstruct, resolver, muleContext);
  }

}
