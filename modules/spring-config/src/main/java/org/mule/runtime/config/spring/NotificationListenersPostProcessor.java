/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Responsible for passing in the MuleContext instance for all objects in the
 * registry that want it. For an object to get an instance of the MuleContext it must
 * implement MuleContextAware.
 * 
 * @see org.mule.runtime.core.api.context.MuleContextAware
 * @see org.mule.runtime.core.api.MuleContext
 * @deprecated as of 3.7.0 because it's not being used. Will be removed in Mule 4.0
 */
@Deprecated
public class NotificationListenersPostProcessor implements BeanPostProcessor
{

    private final MuleContext muleContext;

    public NotificationListenersPostProcessor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof ServerNotificationListener)
        {
            if (!muleContext.getNotificationManager().isListenerRegistered((ServerNotificationListener) bean))
            {
                muleContext.getNotificationManager().addListener((ServerNotificationListener) bean);
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

}
