/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.api.MuleContext;

/**
 * TODO
 */
public abstract class JavaConfigurationBuilder extends AbstractConfigurationBuilder
{
    protected final void doConfigure(MuleContext muleContext) throws Exception
    {
        build();
    }

    protected abstract void build() throws Exception;

    protected ServiceBuilder from(String uri)
    {
    return null;
    }
}
