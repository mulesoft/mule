/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations;

import org.mule.api.annotations.expressions.Function;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;
import org.mule.tck.testmodels.fruit.FruitBowl;


public class AnnotatedComponent1
{
    public Object doSomething(
            @Function("payloadClass") String className,
            @Payload FruitBowl fruitBowl,
            @InboundHeaders("foo") String foo)
    {
        return className + ":" + foo + ":" + fruitBowl.getClass();
    }
}
