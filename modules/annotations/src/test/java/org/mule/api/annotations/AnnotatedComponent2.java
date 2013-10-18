/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations;

import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;

import java.util.Map;

public class AnnotatedComponent2 extends AnnotatedComponent1
{
    public Object doStuff(@Payload String bar, @InboundHeaders("*") Map headers)
    {
        return bar + ":" + headers.toString();
    }

    public String getSomething()
    {
        return "something";
    }

    public String doSomethingElse(Object something)
    {
        return "somethingElse";
    }

    //@Dummy is not a MUle annotation, this method should be ignored
    public Map nonExpressionAnnotation(@Dummy Map map)
    {
        return map;
    }
}
