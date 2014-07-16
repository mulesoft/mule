/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.util.StringDataSource;

import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Tests various cases for how attachments can added to an outbound message through a generic map by injecting the
 * map into a component invocation
 */
public class OutboundAttachmentsAnnotationComponent
{
    public Map<?, ?> processAttachments(@OutboundAttachments Map<String, DataHandler> attachments)
    {
        attachments.put("bar", new DataHandler(new StringDataSource("barValue")));
        //Verify that we receive any outbound attachments already set on the message
        if (attachments.containsKey("foo"))
        {
            //Overwrite the existing attachment to signal that we received it
            attachments.put("foo", new DataHandler(new StringDataSource("fooValue")));
        }
        return attachments;
    }

    //Can only use the {@link OutboundAttachments} annotation on Map parameters
    public List<?> invalidParamType(@OutboundAttachments List<?> attachments)
    {
        return attachments;
    }
}
