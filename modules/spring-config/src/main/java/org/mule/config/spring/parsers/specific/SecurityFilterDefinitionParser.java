/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
