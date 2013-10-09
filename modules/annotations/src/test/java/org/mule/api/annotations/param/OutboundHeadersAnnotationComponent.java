/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import java.util.List;
import java.util.Map;

/**
 * Tests various cases for how headers can added to an outbound message through a generic map by injecting the 
 * map into a component invocation
 */
public class OutboundHeadersAnnotationComponent
{
    public Map<String, Object> processHeaders(@OutboundHeaders Map<String, Object> headers)
    {
        headers.put("bar", "barValue");
        //Verify that we receive any outbound headers already set on the message
        if(headers.containsKey("foo"))
        {
            headers.put("foo", "fooValue");
        }
        return headers;
    }

    //Can only use the {@link OutboundHeaders} annotation on Map parameters
    public List<?> invalidParamType(@OutboundHeaders List<?> headers)
    {
        return headers;
    }
}
