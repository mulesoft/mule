/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
