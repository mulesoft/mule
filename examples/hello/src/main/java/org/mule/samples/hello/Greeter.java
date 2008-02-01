/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.hello;

/**
 * <code>Greeter</code> expects a valid <code>NameString</code> object. If invalid,
 * an exception is created and returned. The outbound router will filter exceptions
 * as user errors and return the messages to the original requester accordingly.
 */
public class Greeter
{
    private String greeting = "";

    public Greeter()
    {
        greeting = LocaleMessage.getGreetingPart1();
    }

    public Object greet(NameString person)
    {
        Object payload = person;
        if (person.isValid())
        {
            person.setGreeting(greeting);
        }
        else
        {
            payload = new Exception(LocaleMessage.getInvalidUserNameError());
        }
        return payload;
    }
}
