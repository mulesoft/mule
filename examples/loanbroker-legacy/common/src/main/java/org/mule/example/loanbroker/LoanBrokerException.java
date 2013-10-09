/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker;

import org.mule.api.MuleException;
import org.mule.config.i18n.MessageFactory;

/**
 * Exception related to the LoanBroker example app.
 */
public class LoanBrokerException extends MuleException
{
    private static final long serialVersionUID = -1669865702115931005L;

    public LoanBrokerException(String message)
    {
        super(MessageFactory.createStaticMessage(message));
    }

    public LoanBrokerException(Exception e)
    {
        super(e);
    }
}
