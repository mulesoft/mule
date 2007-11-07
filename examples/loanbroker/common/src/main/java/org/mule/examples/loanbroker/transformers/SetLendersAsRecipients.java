/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.transformers;

import org.mule.examples.loanbroker.bank.Bank;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.transformers.AbstractMessageAwareTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * Set the Recipient List property on the LoanBrokerQuoteRequest message based on the
 * list of banks in LoanBrokerQuoteRequest.getLenders()
 */
public class SetLendersAsRecipients extends AbstractMessageAwareTransformer
{

    public SetLendersAsRecipients()
    {
        registerSourceType(LoanBrokerQuoteRequest.class);
        // this makes no sense - the code below doesn't change any classes at all...
        //setReturnClass(CustomerQuoteRequest.class);
    }

    public Object transform(UMOMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        Bank[] lenders = ((LoanBrokerQuoteRequest) src).getLenders();

        String recipients = "";
        for (int i = 0; i < lenders.length; i++)
        {
            if (i > 0) recipients += ",";
            recipients += lenders[i].getEndpoint();
        }

        logger.debug("Setting recipients to '" + recipients + "'");
        message.setProperty(StaticRecipientList.RECIPIENTS_PROPERTY, recipients);
        return message;
    }

}
