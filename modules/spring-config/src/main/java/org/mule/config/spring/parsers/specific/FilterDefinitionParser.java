/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.routing.filter.Filter;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.WrappingChildDefinitionParser;
import org.mule.routing.MessageFilter;

import org.w3c.dom.Element;

/**
 * This allows a filter to be defined globally, or embedded within an endpoint. IF
 * required the filter is wrapped in MessageFilter instance before being injected
 * into the parent.
 */
public class FilterDefinitionParser extends ParentContextDefinitionParser
    implements WrappingChildDefinitionParser.WrappingController
{

    public static final String FILTER = "filter";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public FilterDefinitionParser(Class filter)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(filter, false));
        otherwise(new WrappingChildDefinitionParser("messageProcessor", filter, Filter.class, false,
            MessageFilter.class, FILTER, FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public FilterDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(false));
        otherwise(new WrappingChildDefinitionParser("messageProcessor", null, Filter.class, true,
            MessageFilter.class, FILTER, FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public boolean shouldWrap(Element e)
    {
        String parentName = e.getParentNode().getLocalName().toLowerCase();
        String grandParentName = e.getParentNode().getParentNode().getLocalName().toLowerCase();

        return !("message-filter".equals(parentName) || "and-filter".equals(parentName)
                 || "or-filter".equals(parentName) || "not-filter".equals(parentName)
                 || "outbound".equals(grandParentName) || "selective-consumer-router".equals(parentName)
                 || "error-filter".equals(parentName) || "wire-tap".equals(parentName)
                 || "wire-tap-router".equals(parentName) || "when".equals(parentName));
    }
}
