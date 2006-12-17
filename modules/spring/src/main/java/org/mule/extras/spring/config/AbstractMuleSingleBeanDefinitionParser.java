/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.w3c.dom.Element;

import java.util.Properties;

/**
 * todo document
 *
 */
public abstract class AbstractMuleSingleBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    //public static final String ID_ATTRIBUTE = "id";
    //public static final String LOCAL_NAMESPACE = "http://mule.mulesource.org/schema/";

    protected Properties attributeMappings;

    protected AbstractMuleSingleBeanDefinitionParser()
    {
        attributeMappings = new Properties();
    }

  

    protected String extractPropertyName(String attributeName)
    {
        attributeName = getAttributeMapping(attributeName);
        return super.extractPropertyName(attributeName);
    }

    protected abstract Class getBeanClass(Element element);

    protected void postProcess(RootBeanDefinition beanDefinition, Element element) {

    }

    protected void registerAttributeMapping(String alias, String propertyName) {
        attributeMappings.put(alias, propertyName);
    }

    protected String getAttributeMapping(String alias) {
        return attributeMappings.getProperty(alias, alias);
    }
}