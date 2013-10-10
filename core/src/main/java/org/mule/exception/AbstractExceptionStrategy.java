/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
