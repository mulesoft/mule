/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.util.StringUtils;

import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @deprecated Not sure we need this
 * //TODO Try using the MapEntryDefinitionParser instead
 */
public class PropertiesDefinitionParser implements BeanDefinitionParser
{
    private String propertyName = "properties";


    public PropertiesDefinitionParser()
    {
    }

    public PropertiesDefinitionParser(String propertyName)
    {
        this.propertyName = propertyName;
    }

    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MapFactoryBean.class);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        Map parsedMap = parserContext.getDelegate().parseMapElement(element, beanDefinition);
        builder.setSource(parserContext.extractSource(element));
        builder.addPropertyValue("sourceMap", parsedMap);

        //Set on parent
        String parentBean = ((Element) element.getParentNode()).getAttribute("id");
        if (StringUtils.isBlank(parentBean))
        {
            parserContext.getRegistry().registerBeanDefinition(propertyName, beanDefinition);
        }
        else
        {
            BeanDefinition parent = parserContext.getRegistry().getBeanDefinition(parentBean);
            PropertyValue pv = new PropertyValue(propertyName, builder.getBeanDefinition());
            parent.getPropertyValues().addPropertyValue(pv);
        }
        //parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
        // cannot be used in a 'inner-bean' setting (use plain <map>)
        return beanDefinition;
    }

}