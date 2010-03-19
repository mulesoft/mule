/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotificationListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Will register any
 * {@link org.mule.api.context.notification.ServerNotificationListener} instances
 * with the MuleContext to receive notifications
 */
public class NotificationListenerPostProcessor implements BeanPostProcessor
{
    private MuleContext context;

    public NotificationListenerPostProcessor(MuleContext context)
    {
        this.context = context;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof ServerNotificationListener)
        {
            context.getNotificationManager().addListener((ServerNotificationListener) bean);
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
