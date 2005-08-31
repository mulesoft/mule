/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;

/**
 * <code>SyncLoanBroker</code> is a synchronous Loan Broker that
 * makes the calls to various components through the event context
 * synchronously
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SyncLoanBroker 
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SyncLoanBroker.class);

    public UMOMessage getLoanQuote(LoanRequest request) throws Exception {

        logger.info("\nClient " + request.getCustomer().getName() + " with ssn= " + request.getCustomer().getSsn() + " requests a loan of amount= " + request.getLoanAmount() + " for " + request.getLoanDuration() + " months");
        BankQuoteRequest bqr = new BankQuoteRequest();
        bqr.setLoanRequest(request);
        UMOEventContext context = RequestContext.getEventContext();

        //get the customers credit profile
        UMOMessage result = context.sendEvent(request.getCustomer());
        bqr.getLoanRequest().setCreditProfile((CreditProfile)result.getPayload());

        //This asynchronous dispatch will invoke all the bank services concurrently
        //The response of the Banks is handled by the response-router on this component
        //that will block until the requests are received, then aggregate them and
        //send back a response

        //In order for the response router to tie up the response events with this request we must
        //do a couple of things.
        //1. Create our outbound message first before dispatching.  This is so that we can return the
        //the message and the response router can correlate reply messages based on it's Id.
        //2. Call setStopFurtherProcessing() so that Mule knows not to do any more processing of the
        //event
        //3. Must return the dispatched message from this call so that the response transformer can get
        //the message id of the dispatched message. Of course custom implementations can ignore these requirements
        //and implement a custom router that aggregates using something other than the UMOMessage.getUniqueId()

        UMOMessage msg = new MuleMessage(bqr, null);
        //dispatch the message using the default outbound router settings
        context.dispatchEvent(msg);
        context.setStopFurtherProcessing(true);
        return msg;
  }

}
