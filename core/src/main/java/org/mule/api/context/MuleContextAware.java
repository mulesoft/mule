/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context;

import org.mule.api.MuleContext;

/**
 * Objects who want to be aware of the MuleContext should implement this interface. Once the context has
 * been initialised it will be passed to all objects implementing this interface.
 */
public interface MuleContextAware
{
    void setMuleContext(MuleContext context);
}
