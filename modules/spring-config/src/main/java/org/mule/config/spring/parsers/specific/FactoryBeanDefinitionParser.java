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
import org.springframework.beans.factory.FactoryBean;
import org.w3c.dom.Element;

public class FactoryBeanDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private final Class<? extends FactoryBean> factoryBean;

    public FactoryBeanDefinitionParser(Class<? extends FactoryBean> factoryBean)
    {
        this.factoryBean = factoryBean;
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return factoryBean;
    }
}
