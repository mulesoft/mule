/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.client;

import static org.mule.functional.client.TestConnectorConfig.DEFAULT_CONFIG_ID;
import org.mule.DefaultMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;

/**
 * Writes {@link MuleEvent} to a test connector's queue.
 */
public class QueueWriterMessageProcessor implements MessageProcessor, MuleContextAware
{

    private MuleContext muleContext;
    private String name;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        TestConnectorConfig connectorConfig = muleContext.getRegistry().lookupObject(DEFAULT_CONFIG_ID);
        connectorConfig.write(name, DefaultMuleEvent.copy(event));

        return event;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
