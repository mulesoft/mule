/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.QueueStore;
import org.mule.config.QueueProfile;

import java.io.Serializable;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class QueueProfileFactoryBean extends AbstractFactoryBean<QueueProfile> implements MuleContextAware
{
    private int maxOutstandingMessages;
    private MuleContext muleContext;
    private QueueStore<Serializable> queueStore;

    @Override
    public Class<?> getObjectType()
    {
        return QueueProfile.class;
    }

    @Override
    protected QueueProfile createInstance() throws Exception
    {
        QueueStore<Serializable> objectStore = queueStore;
        if (objectStore == null)
        {
            objectStore = muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        }

        return new QueueProfile(getMaxOutstandingMessages(), objectStore);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public int getMaxOutstandingMessages()
    {
        return maxOutstandingMessages;
    }

    public void setMaxOutstandingMessages(int maxOutstandingMessages)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
    }

    public void setQueueStore(QueueStore<Serializable> queueStore)
    {
        this.queueStore = queueStore;
    }

    public QueueStore<Serializable> getQueueStore()
    {
        return queueStore;
    }
}


