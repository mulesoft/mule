/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esn;

import org.mule.examples.loanbroker.DefaultLoanBroker;
import org.mule.examples.loanbroker.LoanBrokerException;
import org.mule.examples.loanbroker.messages.CreditProfile;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * <code>SyncLoanBroker</code> is a synchronous Loan Broker that makes the calls to
 * various components through the event context synchronously.
 */
public class SynchronousLoanBroker extends DefaultLoanBroker
{
    public LoanBrokerQuoteRequest getLoanQuote(CustomerQuoteRequest request) throws LoanBrokerException
    {
        // The Loan Broker request contains the original request from the customer plus additional info.
        LoanBrokerQuoteRequest bqr = super.getLoanQuote(request);
        
    	// Get context used to generate new messages.
        UMOEventContext context = RequestContext.getEventContext();
        
        // get the customers credit profile
        UMOMessage result;
        try
        {
            result = context.sendEvent(bqr);
        }
        catch (UMOException e)
        {
            throw new LoanBrokerException(e);
        }
        if ((result == null) || !(result.getPayload() instanceof CreditProfile))
        {
            throw new LoanBrokerException("No credit profile returned by the credit agency.");
        }
        bqr.setCreditProfile((CreditProfile)result.getPayload());

        // This asynchronous dispatch will invoke all the bank services concurrently
        // The response of the Banks is handled by the response-router on this
        // component
        // that will block until the requests are received, then aggregate them and
        // send back a response

        // In order for the response router to tie up the response events with this
        // request we must
        // do a couple of things.
        // 1. Create our outbound message first before dispatching. This is so that
        // we can return the
        // the message and the response router can correlate reply messages based on
        // it's Id.
        // 2. Call setStopFurtherProcessing() so that Mule knows not to do any more
        // processing of the
        // event
        // 3. Must return the dispatched message from this call so that the response
        // transformer can get
        // the message id of the dispatched message. Of course custom implementations
        // can ignore these requirements
        // and implement a custom router that aggregates using something other than
        // the UMOMessage.getUniqueId()

        try
        {
            context.dispatchEvent(bqr);
        }
        catch (UMOException e)
        {
            throw new LoanBrokerException(e);
        }
        
        context.setStopFurtherProcessing(true);
        return bqr;
    }
}
