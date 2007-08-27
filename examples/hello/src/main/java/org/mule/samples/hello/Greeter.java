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
 * <code>Greeter</code> TODO (document class)
 */
public class Greeter
{
    private String greeting = "";

    public Greeter()
    {
        greeting = LocaleMessage.getGreetingPart1();
    }

    public void greet(NameString person)
    {
        person.setGreeting(greeting);

    }
}
