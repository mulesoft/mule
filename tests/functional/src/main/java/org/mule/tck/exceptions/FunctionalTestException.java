/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.exceptions;

import org.mule.api.MuleException;
import org.mule.config.i18n.MessageFactory;

public class FunctionalTestException extends MuleException
{
    public static final String EXCEPTION_MESSAGE = "Functional Test Service Exception";

    public FunctionalTestException()
    {
        this(EXCEPTION_MESSAGE);
    }

    public FunctionalTestException(String exceptionText)
    {
        super(MessageFactory.createStaticMessage(exceptionText));
    }
}
