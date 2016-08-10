/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.source.StartableCompositeMessageSource;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class CompositeMessageSourceFactoryBean implements FactoryBean
{

    protected List<MessageSource> sources = Collections.<MessageSource> emptyList();

    public Class getObjectType()
    {
        return MessageSource.class;
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
