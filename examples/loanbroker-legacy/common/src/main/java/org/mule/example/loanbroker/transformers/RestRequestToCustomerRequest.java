/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        setReturnDataType(DataTypeFactory.create(CustomerQuoteRequest.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
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

    protected String getParam(MuleMessage message, String name) throws NullPointerException
    {
        String value = message.getOutboundProperty(name);
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter '" + name + "' must be set on the request");
        }
        return value;
    }
}
