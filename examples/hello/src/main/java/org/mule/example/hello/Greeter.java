/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.hello;

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
