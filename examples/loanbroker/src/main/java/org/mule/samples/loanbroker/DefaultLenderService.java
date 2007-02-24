/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker;

import org.mule.RegistryContext;
import org.mule.impl.RequestContext;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultLenderService</code> is responsible for contacting the relevant
 * banks depending on the amount of the loan
 */
public class DefaultLenderService
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void setLenderList(BankQuoteRequest request)
    {
        Bank[] l = getLenderList(request.getLoanRequest().getCreditProfile(), new Double(
            request.getLoanRequest().getLoanAmount()));
        request.setLenders(l);
    }

    public Bank[] getLenderList(CreditProfile creditProfile, Double loanAmount)
    {
        Bank[] lenders;
        if ((loanAmount.doubleValue() >= 20000)) // &&
        // (creditProfile.getCreditScore()
        // >= 600) &&
        // (creditProfile.getCreditHistoryLength()
        // >= 8))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank1", getEndpoint("Bank1Endpoint", "Bank1"));
            lenders[1] = new Bank("Bank2", getEndpoint("Bank2Endpoint", "Bank2"));

        }
        else if (((loanAmount.doubleValue() >= 10000) && (loanAmount.doubleValue() <= 19999))) // &&
        // (creditProfile.getCreditScore()
        // >=
        // 400)
        // &&
        // (creditProfile.getCreditHistoryLength()
        // >=
        // 3))
        {
            lenders = new Bank[2];
            lenders[0] = new Bank("Bank3", getEndpoint("Bank3Endpoint", "Bank3"));
            lenders[1] = new Bank("Bank4", getEndpoint("Bank4Endpoint", "Bank4"));
        }
        else
        {
            lenders = new Bank[1];
            lenders[0] = new Bank("Bank5", getEndpoint("Bank5Endpoint", "Bank5"));
        }

        List recipients = new ArrayList(lenders.length);
        for (int i = 0; i < lenders.length; i++)
        {
            recipients.add(lenders[i].getEndpoint());
        }

        UMOMessage m = RequestContext.getEventContext().getMessage();
        m.setProperty(StaticRecipientList.RECIPIENTS_PROPERTY, recipients);
        return lenders;
    }

    /**
     * A helper method used to make it easier to configure this sample with differet
     * endpoints for testing purposes
     * 
     * @param endpointName
     * @return
     */
    private String getEndpoint(String endpointName, String serviceName)
    {
        UMOEndpoint ep = RegistryContext.getRegistry().lookupEndpoint(endpointName);
        if(ep==null)
        {
            throw new IllegalArgumentException("No endpoint registered called: " + endpointName);
        }

        String endpoint = ep.getEndpointURI().getAddress();

        if ("axis".equals(ep.getEndpointURI().getSchemeMetaInfo()) ||
            "xfire".equals(ep.getEndpointURI().getSchemeMetaInfo()) ||
            "glue".equals(ep.getEndpointURI().getSchemeMetaInfo()) ||
            "soap".equals(ep.getEndpointURI().getSchemeMetaInfo()))
        {
            int i = endpoint.indexOf('?');
            if (i > -1)
            {
                endpoint = endpoint.replaceFirst("\\?", "/" + serviceName + "?method=getLoanQuote\\&");
            }
            else
            {
                endpoint += "/" + serviceName + "?method=getLoanQuote";
            }
        }

        return endpoint;
    }

}
