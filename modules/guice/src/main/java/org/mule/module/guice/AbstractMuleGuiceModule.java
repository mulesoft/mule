/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.context.MuleContextAware;
import org.mule.api.MuleContext;

import com.google.inject.AbstractModule;
import com.google.inject.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * A mule specific Guice module that allows users to override the {@link #configureMuleContext(org.mule.api.MuleContext)} method
 * to do any Mule configuration such as register notifications.  Most users will not need to override this method so the
 * {@link com.google.inject.AbstractModule} can be used.
 *
 * Note that Mule objects such as Connectors and Agents can be registered in a Guice module too.  To do this create provider methods
 * on a module and mark with the {@link com.google.inject.Provides} annotation.
 *
 * Its recommended that you put all your Mule configuration objects in a separate Guice module.
 */
public abstract class AbstractMuleGuiceModule extends AbstractModule
{

    private MuleContext muleContext;

    void setMuleContext(MuleContext context)
    {
        muleContext = context;
        configureMuleContext(muleContext);
    }

    public void configureMuleContext(MuleContext muleContext)
    {

    }
}
