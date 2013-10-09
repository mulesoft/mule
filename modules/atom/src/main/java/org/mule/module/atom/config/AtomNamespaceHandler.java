/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
