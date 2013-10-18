/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
