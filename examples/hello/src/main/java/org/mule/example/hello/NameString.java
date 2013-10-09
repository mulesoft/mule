/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
