/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import static org.mule.config.spring.parsers.specific.ExceptionStrategyDefinitionParser.createNoNameAttributePreProcessor;
import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ReferenceExceptionStrategyDefinitionParser extends ParentContextDefinitionParser
{
    public ReferenceExceptionStrategyDefinitionParser()
    {
        super("configuration", createConfigurationDefinitionParser());
        otherwise(createInFlowServiceDefinitionParser());
    }

    private ParentDefinitionParser createInFlowServiceDefinitionParser()
    {
        ParentDefinitionParser inFlowServiceDefinitionParser = new ParentDefinitionParser();
        inFlowServiceDefinitionParser.addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "exceptionListener");
        inFlowServiceDefinitionParser.registerPreProcessor(createNoNameAttributePreProcessor());
        return inFlowServiceDefinitionParser;
    }

    private static OrphanDefinitionParser createConfigurationDefinitionParser()
    {
        OrphanDefinitionParser orphanDefinitionParser = new OrphanDefinitionParser(false){
            @Override
            protected AbstractBeanDefinition parseInternal(Element element, ParserContext context)
            {
                String ref = element.getAttribute("ref");
                context.getRegistry().registerAlias(ref,MuleProperties.OBJECT_DEFAULT_GLOBAL_EXCEPTION_STRATEGY);
                return null;
            }
        };
        orphanDefinitionParser.registerPreProcessor(createNoNameAttributePreProcessor());
        return orphanDefinitionParser;
    }

}
