/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.ServerNotificationListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Responsible for passing in the MuleContext instance for all objects in the
 * registry that want it. For an object to get an instance of the MuleContext it must
 * implement MuleContextAware.
 * 
 * @see org.mule.api.context.MuleContextAware
 * @see org.mule.api.MuleContext
 */
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
