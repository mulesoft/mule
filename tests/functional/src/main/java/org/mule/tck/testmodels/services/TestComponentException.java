/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.services;

public class TestComponentException extends Exception
{
    private static final long serialVersionUID = -3906931231398539327L;

    public static final String MESSAGE_PREFIX = "Message: ";

    public TestComponentException(String message)
    {
        super(MESSAGE_PREFIX + message);
    }

}
