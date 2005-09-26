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
package org.mule.samples.loanbroker.esb.lender;

import org.mule.MuleManager;
import org.mule.impl.RequestContext;
import org.mule.samples.loanbroker.esb.bank.Bank;
import org.mule.samples.loanbroker.esb.message.CreditProfile;
import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>DefaultLenderService</code> is responsible for contacting the relivant
 * banks depending on the amount of the loan
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LenderService
{
    public void setLenderList(LoanQuoteRequest request)
    {
        Bank[] l = getLenderList(request.getCreditProfile(), new Double(request.getCustomerRequest().getLoanAmount()));
        request.setLenders(l);
    }

    public Bank[] getLenderList(CreditProfile creditProfile, Double loanAmount)
    {
        Bank[] lenders;
        if ((loanAmount.doubleValue() >= (double) 20000))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank1", getEndpoint("Bank1"));
            lenders[1] =new Bank("Bank2", getEndpoint("Bank2"));

        } else if (((loanAmount.doubleValue() >= (double) 10000) && (loanAmount.doubleValue() <= (double) 19999)))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank3", getEndpoint("Bank3"));
            lenders[1] = new Bank("Bank4", getEndpoint("Bank4"));
        } else
        {
            lenders = new Bank[1];
            lenders[0] = new Bank("Bank5", getEndpoint("Bank5"));
        }

        StringBuffer recipients = new StringBuffer();
        for (int i =0;i < lenders.length; i++)
        {
            recipients.append(lenders[i].getEndpoint()).append(",");
        }

        RequestContext.setProperty("recipients", recipients.substring(0, recipients.length()-1));
        return lenders;
    }

    /**
     * A helper method used to make it easier to configure this sample
     * with differet endpoints for testing purposes
     *
     * @param name
     * @return the endpoint for the bank
     */
    private String getEndpoint(String name)
    {
        String endpoint = MuleManager.getInstance().lookupEndpointIdentifier(name, null);
        if (endpoint.startsWith("glue") || endpoint.startsWith("axis") ||
                endpoint.startsWith("soap"))
        {
            int i = endpoint.indexOf('?');
            if(i > -1) {
                endpoint = endpoint.replaceFirst("\\?", "/" + name + "?method=getLoanQuote\\&");
            } else {
                endpoint += "/" + name + "?method=getLoanQuote";
            }
        }
        return endpoint;
    }
}
