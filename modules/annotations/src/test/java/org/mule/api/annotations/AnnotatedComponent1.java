/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
