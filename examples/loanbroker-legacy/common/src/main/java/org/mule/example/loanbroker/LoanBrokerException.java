/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
