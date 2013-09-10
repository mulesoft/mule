/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.module.atom.AbderaServiceComponent;
import org.mule.module.atom.routing.EntryLastUpdatedFilter;
import org.mule.module.atom.routing.FeedLastUpdatedFilter;
import org.mule.module.atom.routing.FeedSplitter;
import org.mule.module.atom.routing.URIRouteFilter;
import org.mule.module.atom.transformers.AtomEntryBuilderTransformer;
import org.mule.module.atom.transformers.ObjectToFeed;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class AtomNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        FilterDefinitionParser routeFilter = new FilterDefinitionParser(URIRouteFilter.class);
        registerBeanDefinitionParser("route-filter", routeFilter);

        registerBeanDefinitionParser("entry-last-updated-filter", new FilterDefinitionParser(EntryLastUpdatedFilter.class));
        registerBeanDefinitionParser("feed-last-updated-filter", new FilterDefinitionParser(FeedLastUpdatedFilter.class));
        registerBeanDefinitionParser("feed-splitter", new RouterDefinitionParser(FeedSplitter.class));
        registerBeanDefinitionParser("component", new ComponentDefinitionParser(AbderaServiceComponent.class));
        registerBeanDefinitionParser("entry-builder-transformer", new MessageProcessorDefinitionParser(AtomEntryBuilderTransformer.class));
        registerBeanDefinitionParser("entry-property", new ChildDefinitionParser("argument", ExpressionArgument.class));
        registerBeanDefinitionParser("object-to-feed-transformer", new MessageProcessorDefinitionParser(ObjectToFeed.class));
    }
}
