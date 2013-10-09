/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.junit4.rule;

import org.junit.rules.ExternalResource;

/**
 * Sets up a system property before a test and guaranties to tear it down
 * afterward.
 */
public class SystemProperty extends ExternalResource
{

    private final String name;
    private String value;
    private boolean initialized;
    private String oldValue;

    public SystemProperty(String name)
    {
        this(name, null);
    }

    public SystemProperty(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    protected void before() throws Throwable
    {
        if (initialized)
        {
            throw new IllegalArgumentException("System property was already initialized");
        }

        oldValue = System.setProperty(name, getValue());
        initialized = true;
    }

    @Override
    protected void after()
    {
        if (!initialized)
        {
            throw new IllegalArgumentException("System property was not initialized");
        }

        doCleanUp();
        restoreOldValue();

        initialized = false;
    }

    protected void restoreOldValue()
    {
        if (oldValue == null)
        {
            System.clearProperty(name);
        }
        else
        {
            System.setProperty(name, oldValue);
        }
    }

    public String getName()
    {
        return name;
    }

    protected void doCleanUp()
    {
        // Nothing to do
    };

    public String getValue()
    {
        return value;
    };
}
