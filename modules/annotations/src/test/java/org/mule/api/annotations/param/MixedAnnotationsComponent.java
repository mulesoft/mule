/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
