/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.processor.DevkitBasedMessageProcessor;
import org.mule.security.oauth.OAuth1Adapter;

public abstract class BaseOAuth1UnauthorizeMessageProcessor extends DevkitBasedMessageProcessor
    implements MessageProcessor
{

    public BaseOAuth1UnauthorizeMessageProcessor()
    {
        super("unauthorize");
    }

    /**
     * Unauthorize the connector
     * 
     * @param event MuleEvent to be processed
     * @throws Exception
     */
    @Override
    protected MuleEvent doProcess(MuleEvent event) throws Exception
    {
        OAuth1Adapter adapter = this.getAdapter();
        adapter.reset();

        return event;
    }

    protected abstract Class<? extends OAuth1Adapter> getAdapterClass();

    protected OAuth1Adapter getAdapter()
    {
        try
        {
            Object maybeAnAdapter = this.findOrCreate(this.getAdapterClass(), false, null);
            if (!(maybeAnAdapter instanceof OAuth1Adapter))
            {
                throw new IllegalStateException(String.format(
                    "Object of class %s does not implement OAuth1Adapter", this.getAdapterClass()
                        .getCanonicalName()));
            }

            return (OAuth1Adapter) maybeAnAdapter;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
