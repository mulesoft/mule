/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.interceptor.InterceptorStack;

/**
 * This allows a interceptor-stack to be defined globally, or configured on a
 * service.
 */
public class InterceptorStackDefinitionParser extends ParentContextDefinitionParser
{
    public static final String INTERCEPTOR_STACK = "interceptor";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    /**
     * For custom transformers
     */
    public InterceptorStackDefinitionParser()
    {
        // Interceptor stacks get next message processor etc. set in their chains and thus
        // cannot be singletons
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, 
            new MuleOrphanDefinitionParser(InterceptorStack.class, false));
        otherwise(addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF,
            INTERCEPTOR_STACK));
        super.addIgnored(ATTRIBUTE_NAME);
    }

    private static MuleDefinitionParser addAlias(MuleDefinitionParser parser, String alias, String name)
    {
        parser.addAlias(alias, name);
        return parser;
    }
}
