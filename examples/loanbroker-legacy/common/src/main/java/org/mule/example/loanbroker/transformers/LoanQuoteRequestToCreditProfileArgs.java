/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        registerSourceType(DataTypeFactory.create(LoanBrokerQuoteRequest.class));
        setReturnDataType(DataTypeFactory.create(Object[].class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        LoanBrokerQuoteRequest request = (LoanBrokerQuoteRequest)src;
        Object[] args = new Object[2];
        args[0] = request.getCustomerRequest().getCustomer().getName();
        args[1] = new Integer(request.getCustomerRequest().getCustomer().getSsn());
        return args;
    }

}
