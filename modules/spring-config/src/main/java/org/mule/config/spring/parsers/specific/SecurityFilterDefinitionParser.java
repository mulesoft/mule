/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.security.SecurityFilter;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.WrappingChildDefinitionParser;
import org.mule.processor.SecurityFilterMessageProcessor;

import org.w3c.dom.Element;

/**
 * This allows a security filter to be defined globally, or embedded within an endpoint. The filter is
 * always wrapped in a SecurityFilterMessageProcessorBuilder instance before being injected into the parent.
 */
public class SecurityFilterDefinitionParser extends ParentContextDefinitionParser  implements WrappingChildDefinitionParser.WrappingController
{

    public static final String SECURITY_FILTER = "filter";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public SecurityFilterDefinitionParser(Class filter)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(filter, false));
        otherwise(
            new WrappingChildDefinitionParser(
                "messageProcessor", filter, SecurityFilter.class, false, SecurityFilterMessageProcessor.class,
                SECURITY_FILTER, SECURITY_FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public SecurityFilterDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(false));
        otherwise(
            new WrappingChildDefinitionParser(
                "messageProcessor", null, SecurityFilter.class, true, SecurityFilterMessageProcessor.class,
                SECURITY_FILTER, SECURITY_FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public boolean shouldWrap(Element elm)
    {
        return true;
    }
}
