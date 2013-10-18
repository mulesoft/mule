/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.config;

import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.module.rss.routing.EntryLastUpdatedFilter;
import org.mule.module.rss.routing.FeedLastUpdatedFilter;
import org.mule.module.rss.routing.FeedSplitter;
import org.mule.module.rss.transformers.ObjectToRssFeed;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class RssNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("feed-splitter", new RouterDefinitionParser(FeedSplitter.class));
        registerBeanDefinitionParser("entry-last-updated-filter", new FilterDefinitionParser(EntryLastUpdatedFilter.class));
        registerBeanDefinitionParser("feed-last-updated-filter", new FilterDefinitionParser(FeedLastUpdatedFilter.class));
        registerBeanDefinitionParser("object-to-feed-transformer", new MessageProcessorDefinitionParser(ObjectToRssFeed.class));
    }
}
