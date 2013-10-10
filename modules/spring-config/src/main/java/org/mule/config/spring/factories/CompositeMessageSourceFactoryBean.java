/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.source.StartableCompositeMessageSource;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class CompositeMessageSourceFactoryBean implements FactoryBean
{

    protected List<MessageSource> sources = Collections.<MessageSource> emptyList();

    public Class getObjectType()
    {
        return MessageProcessor.class;
    }

    public void setMessageSources(List<MessageSource> sources)
    {
        this.sources = sources;
    }

    public Object getObject() throws Exception
    {
        CompositeMessageSource composite = new StartableCompositeMessageSource();
        for (MessageSource source : sources)
        {
            composite.addSource(source);
        }
        return composite;
    }

    public boolean isSingleton()
    {
        return false;
    }

}
