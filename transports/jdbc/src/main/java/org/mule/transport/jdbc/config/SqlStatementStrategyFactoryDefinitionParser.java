/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SqlStatementStrategyFactoryDefinitionParser extends AbstractBeanDefinitionParser
{

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        final MutablePropertyValues parentProps = parserContext.getContainingBeanDefinition().getPropertyValues();

        final String ref = element.getAttribute("ref");
        final String clazz = element.getAttribute("class");
        if (StringUtils.isBlank(ref) && StringUtils.isBlank(clazz))
        {
            throw new IllegalArgumentException("Neither ref nor class attribute specified for the sqlStatementStrategyFactory element");
        }


        if (StringUtils.isNotBlank(ref))
        {
            // add a ref to other bean
            parentProps.addPropertyValue(JdbcNamespaceHandler.ATTRIBUTE_SQL_STATEMENT_FACTORY, new RuntimeBeanReference(ref));
        }
        else
        {
            // class attributed specified, instantiate and set directly
            final Object strategy;
            try
            {
                strategy = ClassUtils.instanciateClass(clazz, ClassUtils.NO_ARGS, getClass());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            parentProps.addPropertyValue(JdbcNamespaceHandler.ATTRIBUTE_SQL_STATEMENT_FACTORY, strategy);
        }

        return null;
    }

}
