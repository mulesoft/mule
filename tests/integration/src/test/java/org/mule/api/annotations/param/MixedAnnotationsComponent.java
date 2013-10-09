/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * A component for testing invocations with more than one parameter
 */
public class MixedAnnotationsComponent
{
    public Map<?, ?> processAllAnnotated(@Payload String payload, 
        @InboundHeaders("foo, bar") Map<?, ?> headers, 
        @InboundAttachments("*") Map<String, DataHandler> attachments)
    {
        Map<String, Object> m = new HashMap<String, Object>(3);
        m.put("payload", payload);
        m.put("inboundHeaders", headers);
        m.put("inboundAttachments", attachments);
        return m;
    }

    public Map<?, ?> processPayloadNotAnnotated(String payload, 
        @InboundHeaders("foo, bar") Map<?, ?> headers, 
        @InboundAttachments("*") List<DataHandler> attachments)
    {
        Map<String, Object> m = new HashMap<String, Object>(3);
        m.put("payload", payload);
        m.put("inboundHeaders", headers);
        m.put("inboundAttachments", attachments);
        return m;
    }
}
