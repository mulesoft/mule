/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.handlers;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Associate a priority with a handler.
 * Higher priority handlers for the same namespace override lower priority handlers.
 *
 * {@see org.mule.config.spring.MuleNamespaceHandlerResolver}
 */
public abstract class AbstractPriorityNamespaceHandler extends NamespaceHandlerSupport
{

    public static final int DEFAULT_PRIORITY = 0;

    private int priority = DEFAULT_PRIORITY;

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

}
