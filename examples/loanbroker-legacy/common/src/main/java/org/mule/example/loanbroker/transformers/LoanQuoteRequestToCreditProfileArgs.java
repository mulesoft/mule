/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * Extracts the customer information from the request into an array of arguments used
 * to invoke the Credit Agency MuleSession bean
 */
public class LoanQuoteRequestToCreditProfileArgs extends AbstractTransformer
{
    public LoanQuoteRequestToCreditProfileArgs()
    {
        super();
        registerSourceType(DataTypeFactory.create(LoanBrokerQuoteRequest.class));
        setReturnDataType(DataTypeFactory.create(Object[].class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        LoanBrokerQuoteRequest request = (LoanBrokerQuoteRequest)src;
        Object[] args = new Object[2];
        args[0] = request.getCustomerRequest().getCustomer().getName();
        args[1] = new Integer(request.getCustomerRequest().getCustomer().getSsn());
        return args;
    }
}
