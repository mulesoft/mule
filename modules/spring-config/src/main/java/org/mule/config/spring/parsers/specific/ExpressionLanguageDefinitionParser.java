/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.el.mvel.MVELExpressionLanguageWrapper;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ExpressionLanguageDefinitionParser extends NamedDefinitionParser
{

    public ExpressionLanguageDefinitionParser()
    {
        super(MuleProperties.OBJECT_EXPRESSION_LANGUAGE);
        singleton = true;
    }

    protected Class getBeanClass(Element element)
    {
        return MVELExpressionLanguageWrapper.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
        throws BeanDefinitionStoreException
    {
        return MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
    }

}
