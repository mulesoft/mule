/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.example;

import java.util.Locale;

/**
 * A simple greeting service that will return a greeting in the supplied language locale
 */
public class AnnotatedGreetingComponent implements LanguageService
{
    //@Receive(uri = "${greeter.endpoint}")
    public String getGreeting(Locale locale) throws LanguageNotSupportedException
    {
        if (locale.equals(Locale.US))
        {
            return "Howdy";
        }
        else if (locale.equals(Locale.ENGLISH))
        {
            return "Good day to you";
        }
        else if (locale.equals(Locale.FRENCH))
        {
            return "Bon jour";
        }
        else if (locale.equals(Locale.GERMAN))
        {
            return "Guten tag";
        }
        else
        {
            throw new LanguageNotSupportedException(locale.toString());
        }

    }

    public String sayHello()
    {
        return "Hello";
    }
}
