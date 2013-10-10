/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import static org.mule.config.spring.parsers.specific.ExceptionStrategyDefinitionParser.createNoNameAttributePreProcessor;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

public class ReferenceExceptionStrategyDefinitionParser extends ParentDefinitionParser
{
    public ReferenceExceptionStrategyDefinitionParser()
    {
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "exceptionListener");
        registerPreProcessor(createNoNameAttributePreProcessor());
    }

}
