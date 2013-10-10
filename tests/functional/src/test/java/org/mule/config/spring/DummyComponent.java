/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.context.MuleContextAware;

public class DummyComponent
{
    private MuleContextAware property;

    public MuleContextAware getProperty()
    {
        return property;
    }

    public void setProperty(MuleContextAware dummy)
    {
        this.property = dummy;
    }

}
