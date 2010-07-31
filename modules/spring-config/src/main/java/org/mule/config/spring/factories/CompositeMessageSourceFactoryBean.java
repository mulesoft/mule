/*
 * $Id: OutboundEndpointFactoryBean.java 11079 2008-02-27 15:52:01Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
