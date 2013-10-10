/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice;

import org.mule.tck.testmodels.fruit.Apple;

/**
 * A test service
 */
public class DefaultAutoTransformService implements AutoTransformServiceInterface
{
    //An Orange is sent but we recieve an Apple through auto transformation
    public Object doSomething(Apple data) throws Exception
    {
        return data;
    }
}
