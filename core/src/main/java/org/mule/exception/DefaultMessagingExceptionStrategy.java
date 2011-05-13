/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import org.mule.api.MuleContext;

/**
 * <code>DefaultServiceExceptionStrategy</code> is the default exception handler
 * for flows. The handler logs errors and will forward the message and exception
 * to an exception endpointUri if one is set on this Exception strategy
 */
public class DefaultMessagingExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    /** 
     * For IoC only 
     * @deprecated Use DefaultServiceExceptionStrategy(MuleContext muleContext) instead
     */
    public DefaultMessagingExceptionStrategy()
    {
        super();
    }

    public DefaultMessagingExceptionStrategy(MuleContext muleContext)
    {
        super();
        setMuleContext(muleContext);
    }
}
