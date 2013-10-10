/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
