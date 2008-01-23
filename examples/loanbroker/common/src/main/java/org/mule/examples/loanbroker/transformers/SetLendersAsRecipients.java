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

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.examples.loanbroker.bank.Bank;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.transformer.AbstractMessageAwareTransformer;

/**
 * Set the Recipient List property on the LoanBrokerQuoteRequest message based on the
 * list of banks in LoanBrokerQuoteRequest.getLenders()
 */
public class SetLendersAsRecipients extends AbstractMessageAwareTransformer
{

    public SetLendersAsRecipients()
    {
        this.registerSourceType(LoanBrokerQuoteRequest.class);
        this.setReturnClass(MuleMessage.class);
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
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
