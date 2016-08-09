/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.component;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.model.EntryPointResolverSet;

/** <code>LifecycleAdapterFactory</code> TODO (document class) */
public interface LifecycleAdapterFactory<JC extends JavaComponent> {

  LifecycleAdapter create(Object pojoService, JC component, FlowConstruct flowConstruct, EntryPointResolverSet resolver,
                          MuleContext muleContext)
      throws MuleException;
}
