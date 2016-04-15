/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.param;

import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class InputParamDefinitionDefinitionParser extends AbstractParamDefinitionDefinitionParser
{

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return DefaultInputQueryParam.class;
    }


    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        builder.addConstructorArgValue(getListElementIndex(element));
        builder.addConstructorArgValue(getType(element));
        builder.addConstructorArgValue(getValue(element));
        builder.addConstructorArgValue(getName(element));
    }

}
