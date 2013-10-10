/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleEventContext;
import org.mule.util.concurrent.Latch;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.springframework.aop.MethodBeforeAdvice;

public class FunctionalTestAdvice implements MethodBeforeAdvice
{

    private Latch latch = new Latch();
    private String message;

    public void before(Method method, Object[] args, Object target) throws Throwable
    {
        if (null != args && args.length == 1 && args[0] instanceof MuleEventContext)
        {
            message = ((MuleEventContext) args[0]).getMessageAsString();
        }
        latch.countDown();
    }

    public String getMessage(long ms) throws InterruptedException
    {
        latch.await(ms, TimeUnit.MILLISECONDS);
        return message;
    }

}
