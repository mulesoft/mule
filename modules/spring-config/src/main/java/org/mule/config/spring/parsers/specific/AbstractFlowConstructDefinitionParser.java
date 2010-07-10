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

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractFlowConstructDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private static final String ABSTRACT_ATTRIBUTE = "abstract";
    private static final String PARENT_ATTRIBUTE = "parent";

    // TODO support default service exception strategy
    // TODO support injection of exception strategy

    @SuppressWarnings("unchecked")
    @Override
    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        return BeanDefinitionBuilder.genericBeanDefinition(beanClass);
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        handleAbstractAttribute(element, builder);
        handleParentAttribute(element, builder);
        super.doParse(element, parserContext, builder);
    }

    private void handleParentAttribute(Element element, BeanDefinitionBuilder builder)
    {
        String parentAttribute = element.getAttribute(PARENT_ATTRIBUTE);
        if (StringUtils.isNotBlank(parentAttribute))
        {
            builder.setParentName(parentAttribute);
        }
        element.removeAttribute(PARENT_ATTRIBUTE);
    }

    private void handleAbstractAttribute(Element element, BeanDefinitionBuilder builder)
    {
        String abstractAttribute = element.getAttribute(ABSTRACT_ATTRIBUTE);
        if (StringUtils.isNotBlank(abstractAttribute))
        {
            builder.setAbstract(Boolean.parseBoolean(abstractAttribute));
        }
        element.removeAttribute(ABSTRACT_ATTRIBUTE);
    }
}
