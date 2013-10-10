/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NameableObject;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

public class TestMessageProcessor implements MessageProcessor, NameableObject
{
    /** Simple label string to be appended to the payload. */
    private String label;
    
    /** Bean name used by Spring */
    private String name;
    
    public TestMessageProcessor()
    {
        // For IoC
    }
    
    public TestMessageProcessor(String label)
    {
        this.label = label;
    }
    
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && event.getMessage() != null)
        {
            event.getMessage().setPayload(event.getMessage().getPayload() + ":" + label);
        }
        return event;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}


