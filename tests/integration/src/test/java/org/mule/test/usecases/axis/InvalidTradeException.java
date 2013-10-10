/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis;

/**
 * TODO document
 */
public class InvalidTradeException extends Exception
{
    private static final long serialVersionUID = -997233549872918131L;

    public InvalidTradeException()
    {
        super();
    }

    public InvalidTradeException(String message)
    {
        super(message);
    }

    public InvalidTradeException(Throwable cause)
    {
        super(cause);
    }

    public InvalidTradeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
