/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

public class TestMessageProcessor implements MessageProcessor
{
    private String label;
    
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
        event.getMessage().setPayload(event.getMessage().getPayload() + ":" + label);
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
}


