/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.events.async;

import org.mule.module.spring.events.MuleApplicationEvent;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <code>OrderManagerBean</code> receives order beans from Mule and dispatches
 * process relsults back through Mule via the applicationContext.
 */
public class AsyncOrderManagerBean extends OrderManagerBean
    implements AsyncOrderManager, ApplicationContextAware
{
    private ApplicationContext applicationContext;

    /**
     * We need the application context to send our asyncronous result somewhere
     * 
     * @param applicationContext
     * @throws BeansException
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * This will be invoked using as a web service. We'll return the result to a
     * processing queue
     * 
     * @param order
     */
    public void processOrderAsync(Order order)
    {
        // Do some processing...
        String message = "Order '" + order.getOrder() + "' Processed Async";
        MuleApplicationEvent returnEvent = null;
        returnEvent = new MuleApplicationEvent(message, "jms://processed.queue");

        // Call publish on the application context, Mule will do the rest
        applicationContext.publishEvent(returnEvent);
    }
}
