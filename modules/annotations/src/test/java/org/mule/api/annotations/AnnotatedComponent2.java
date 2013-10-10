/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
