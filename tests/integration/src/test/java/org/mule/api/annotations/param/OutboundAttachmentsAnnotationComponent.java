/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
