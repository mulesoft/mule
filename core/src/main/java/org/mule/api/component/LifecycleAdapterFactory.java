/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.component;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.model.EntryPointResolverSet;

/** <code>LifecycleAdapterFactory</code> TODO (document class) */
public interface LifecycleAdapterFactory
{
    LifecycleAdapter create(Object pojoService,
                            JavaComponent component,
                            FlowConstruct flowConstruct,
                            EntryPointResolverSet resolver,
                            MuleContext muleContext) throws MuleException;
}
