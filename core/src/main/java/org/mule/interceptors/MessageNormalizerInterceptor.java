/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptors;

import org.mule.config.MuleProperties;
import org.mule.impl.RequestContext;
import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

/**
 * <code>MessageNormalizerInterceptor</code> can be used as a simple pre/post message
 * normalizer for a given component. This is useful in situations where you have an
 * existing component that may accept a one or more child objects of the incoming object.
 * For example, you may Have a BankQuoteRequest object that contains customer, credit and
 * loan details, but one component is only interested in enriching the customer
 * information. Rather than have your component understand how to deal with a
 * BankLoanRequest this interceptor can be used to extract the customer and pass that to
 * the component. Once the component have finshed processing this interceptor update the
 * BankLoanRequest with the new customer information.
 */
public abstract class MessageNormalizerInterceptor implements UMOInterceptor
{
    private Object originalPayload = null;

    /**
     * This method is invoked before the event is processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract UMOMessage before(Invocation invocation) throws UMOException;

    /**
     * This method is invoked after the event has been processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract UMOMessage after(Invocation invocation) throws UMOException;

    public final UMOMessage intercept(Invocation invocation) throws UMOException
    {
        // store the original payload as we will need it later
        originalPayload = invocation.getEvent().getTransformedMessage();

        // get the updated message
        UMOMessage bMessage = before(invocation);
        if (bMessage != null)
        {
            // update the current event
            RequestContext.rewriteEvent(bMessage);
            // update the message in the invocation
            invocation.setMessage(bMessage);
            // remove any method override as it will not apply to the new
            // message payload
            invocation.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);
        }
        // invoke
        UMOMessage message = invocation.execute();
        // Update the message
        invocation.setMessage(message);
        UMOMessage aMessage = after(invocation);
        if (aMessage == null)
        {
            return message;
        }
        else
        {
            return aMessage;
        }
    }

    protected Object getOriginalPayload()
    {
        return originalPayload;
    }

    protected void setOriginalPayload(Object originalPayload)
    {
        this.originalPayload = originalPayload;
    }
}
