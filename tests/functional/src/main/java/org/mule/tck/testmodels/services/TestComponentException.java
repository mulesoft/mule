/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
