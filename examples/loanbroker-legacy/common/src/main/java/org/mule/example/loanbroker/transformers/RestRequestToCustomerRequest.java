/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * Converts parameters on the message into a CustomerQuoteRequest object
 */
public class RestRequestToCustomerRequest extends AbstractMessageTransformer
{
    public RestRequestToCustomerRequest()
    {
        super();
        setReturnDataType(DataTypeFactory.create(CustomerQuoteRequest.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        String customerName;
        int ssn;
        double amount;
        int duration;

        try
        {
            customerName = getParam(message, "customerName");
            ssn = Integer.parseInt(getParam(message, "ssn"));
            amount = Double.parseDouble(getParam(message, "loanAmount"));
            duration = Integer.parseInt(getParam(message, "loanDuration"));
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        Customer c = new Customer(customerName, ssn);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, amount, duration);
        return request;
    }

    protected String getParam(MuleMessage message, String propertyName) throws NullPointerException
    {
        String value = message.getOutboundProperty(propertyName);
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter '" + propertyName + "' must be set on the request");
        }
        return value;
    }
}
