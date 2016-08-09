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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.component.DefaultComponentLifecycleAdapter;

/** <code>TestComponentLifecycleAdapter</code> TODO document */
public class TestComponentLifecycleAdapter extends DefaultComponentLifecycleAdapter {

  public TestComponentLifecycleAdapter(Object pojoService, JavaComponent service, FlowConstruct flowConstruct,
                                       MuleContext muleContext)
      throws MuleException {
    super(pojoService, service, flowConstruct, muleContext);
  }

  public TestComponentLifecycleAdapter(Object pojoService, JavaComponent service, FlowConstruct flowConstruct,
                                       EntryPointResolverSet epResolver, MuleContext muleContext)
      throws MuleException {
    super(pojoService, service, flowConstruct, epResolver, muleContext);
  }

}
