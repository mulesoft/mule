/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.param;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.AutoIdUtils;

import org.mule.module.db.internal.domain.query.QueryParamValue;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class InputParamValueBeanDefinitionParser extends AbstractMuleBeanDefinitionParser
{

    public static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String NAME_ATTRIBUTE = "name";

    public InputParamValueBeanDefinitionParser()
    {
        addAlias("defaultValue", "value");
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return QueryParamValue.class;
    }

    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        builder.addConstructorArgValue(getName(element));
        builder.addConstructorArgValue(getValue(element));
    }

    private String getValue(Element element)
    {
        String value;
        if (element.hasAttribute(DEFAULT_VALUE_ATTRIBUTE))
        {
            value = element.getAttribute(DEFAULT_VALUE_ATTRIBUTE);
        }
        else
        {
            value = element.getAttribute(VALUE_ATTRIBUTE);
        }
        return value;
    }

    private String getName(Element element)
    {
        return element.getAttribute(NAME_ATTRIBUTE);
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext context) throws BeanDefinitionStoreException
    {
        return getBeanName(element);
    }

    @Override
    public String getBeanName(Element element)
    {
        return AutoIdUtils.uniqueValue("paramValue." + element.getAttribute(ATTRIBUTE_NAME));
    }

    @Override
    protected void checkElementNameUnique(Element element)
    {
        // Don't care about this
    }

    @Override
    protected boolean isSingleton()
    {
        return true;
    }
}
