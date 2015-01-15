/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import static org.mule.module.springconfig.parsers.specific.ExceptionStrategyDefinitionParser.createNoNameAttributePreProcessor;

import org.mule.module.springconfig.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.module.springconfig.parsers.generic.ParentDefinitionParser;

public class ReferenceExceptionStrategyDefinitionParser extends ParentDefinitionParser
{
    public ReferenceExceptionStrategyDefinitionParser()
    {
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "exceptionListener");
        registerPreProcessor(createNoNameAttributePreProcessor());
    }

}
