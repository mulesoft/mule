/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


