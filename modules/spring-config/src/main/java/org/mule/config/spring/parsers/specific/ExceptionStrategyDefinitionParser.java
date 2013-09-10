/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.IdAttribute;
import org.mule.config.spring.parsers.processors.NameAttribute;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ExceptionStrategyDefinitionParser extends ParentContextDefinitionParser
{
    public ExceptionStrategyDefinitionParser(Class exceptionStrategyClass)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, createRootDefinitionParser(exceptionStrategyClass));
        otherwise(createInFlowServiceDefinitionParser(exceptionStrategyClass));
    }

    private ChildDefinitionParser createInFlowServiceDefinitionParser(Class exceptionStrategyClass)
    {
        ChildDefinitionParser exceptionListenerDefinitionParser = new ChildDefinitionParser("exceptionListener", exceptionStrategyClass, false);
        exceptionListenerDefinitionParser.registerPreProcessor(createNoNameAttributePreProcessor());
        return exceptionListenerDefinitionParser;
    }

    static PreProcessor createNoNameAttributePreProcessor()
    {
        return new PreProcessor()
        {
            @Override
            public void preProcess(PropertyConfiguration config, Element element)
            {
                if (element.hasAttribute("name"))
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("name attribute on exception strategy is only allowed on global exception strategies"));
                }
            }
        };
    }

    public static MuleOrphanDefinitionParser createRootDefinitionParser(Class exceptionStrategyClass)
    {
        MuleOrphanDefinitionParser globalExceptionStrategyDefinitionParser;
        if (exceptionStrategyClass == null)
        {
            globalExceptionStrategyDefinitionParser = new MuleOrphanDefinitionParser(false);
        }
        else
        {
            globalExceptionStrategyDefinitionParser = new MuleOrphanDefinitionParser(exceptionStrategyClass, false);
        }
        globalExceptionStrategyDefinitionParser.addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        return globalExceptionStrategyDefinitionParser;
    }

}
