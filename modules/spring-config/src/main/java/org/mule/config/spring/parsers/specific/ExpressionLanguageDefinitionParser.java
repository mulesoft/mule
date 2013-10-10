/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.el.mvel.MVELExpressionLanguage;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ExpressionLanguageDefinitionParser extends NamedDefinitionParser
{

    public static final String DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE = "defaultExceptionStrategy-ref";

    public ExpressionLanguageDefinitionParser()
    {
        super(MuleProperties.OBJECT_EXPRESSION_LANGUAGE);
        singleton = true;
    }

    protected Class getBeanClass(Element element)
    {
        return MVELExpressionLanguage.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
        throws BeanDefinitionStoreException
    {
        return MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
    }

}
