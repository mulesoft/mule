/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.hello;

import java.io.Serializable;

/**
 * <code>NameString</code> is a simple string wrapper that holds a name and a greeting string
 */
public class NameString implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7010138636008560022L;

    private String name;
    private String greeting;

    public NameString()
    {
        this.name = null;
    }
    
    public NameString(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the greeting.
     */
    public String getGreeting()
    {
        return greeting;
    }

    /**
     * @param greeting The greeting to set.
     */
    public void setGreeting(String greeting)
    {
        this.greeting = greeting;
    }
    
    public boolean isValid()
    {
        return name != null && name.length() > 0;
    }
}
