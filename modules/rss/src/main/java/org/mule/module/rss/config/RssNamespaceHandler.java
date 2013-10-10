/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
