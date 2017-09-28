/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * A {@link BeanPostProcessor} which registers {@link NotificationListener} objects into the
 * {@link ServerNotificationManager}
 *
 * @since 3.7.0
 */
public class NotificationListenerPostProcessor implements BeanPostProcessor {

  private final MuleContext muleContext;

  public NotificationListenerPostProcessor(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof NotificationListener) {
      muleContext.getNotificationManager().addListener((NotificationListener) bean);
    }

    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }
}
