/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


