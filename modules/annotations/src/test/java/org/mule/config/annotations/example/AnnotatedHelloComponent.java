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


import org.mule.api.NamedObject;
import org.mule.config.annotations.endpoints.Bind;

import java.util.Locale;

/**
 * This annotate service provides all the details Mule needs to configure a service without any
 * additional configuration!
 */
public class AnnotatedHelloComponent implements NamedObject
{
    @Bind(uri = "${greeter.endpoint}")
    private transient LanguageService languageService;

    private Locale messageLocale = Locale.getDefault();

    private String name;

    //@Receive(uri = "${hello.endpoint}", id = "helloEndpoint")
    public String hello(String name)
    {
        try
        {
            String greeting = languageService.getGreeting(getMessageLocale());
            return greeting + " " + name;
        }
        catch (LanguageNotSupportedException e)
        {
            return "Sorry we couldn't greet you in your local languange: " + getMessageLocale();
        }
    }

    public LanguageService getLanguageService()
    {
        return languageService;
    }

    public void setLanguageService(LanguageService languageService)
    {
        this.languageService = languageService;
    }

    public Locale getMessageLocale()
    {
        return messageLocale;
    }

    //Only needed for testing but Mule could automatically injects a Mock proxy as part of the test case
    //Though the user may want to assert the invocation.  Need to think of a clean way of mockig out endpoints in tests
    public void setMessageLocale(Locale messageLocale)
    {
        this.messageLocale = messageLocale;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
