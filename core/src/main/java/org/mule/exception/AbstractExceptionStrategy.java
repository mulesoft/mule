/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleContext;

/**
 * This is the base class for exception strategies which contains several helper methods.  However, you should 
 * probably inherit from <code>AbstractMessagingExceptionStrategy</code> (if you are creating a Messaging Exception Strategy) 
 * or <code>AbstractSystemExceptionStrategy</code> (if you are creating a System Exception Strategy) rather than directly from this class.
 *
 * @deprecated use {@link org.mule.exception.AbstractExceptionListener}
 */
@Deprecated
public abstract class AbstractExceptionStrategy extends AbstractExceptionListener
{
    public AbstractExceptionStrategy(MuleContext muleContext)
    {
        setMuleContext(muleContext);
    }

}
