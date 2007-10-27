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

import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.transformers.AbstractMessageAwareTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * Converts parameters on the message into a CustomerQuoteRequest object
 */
public class RestRequestToCustomerRequest extends AbstractMessageAwareTransformer
{

    public RestRequestToCustomerRequest()
    {
        setReturnClass(CustomerQuoteRequest.class);
    }

    public Object transform(UMOMessage message, String outputEncoding) throws TransformerException
    {
        String name;
        int ssn;
        double amount;
        int duration;

        try
        {
            name = getParam(message, "customerName");
            ssn = Integer.parseInt(getParam(message, "ssn"));
            amount = Double.parseDouble(getParam(message, "loanAmount"));
            duration = Integer.parseInt(getParam(message, "loanDuration"));
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        Customer c = new Customer(name, ssn);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, amount, duration);
        return request;
    }

    protected String getParam(UMOMessage message, String name) throws NullPointerException
    {
        String value = message.getStringProperty(name, null);
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter '" + name + "' must be set on the request");
        }
        return value;
    }
}
