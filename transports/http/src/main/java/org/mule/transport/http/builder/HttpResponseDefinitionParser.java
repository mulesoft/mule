/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.builder;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class HttpResponseDefinitionParser extends ChildDefinitionParser
{

    public HttpResponseDefinitionParser(String setterMethod)
    {
        super(setterMethod, ManagedMap.class);
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        ManagedMap values = new ManagedMap();

        values.put(processHeaderName(element.getLocalName()), element.getAttribute("value"));

        builder.addPropertyValue("sourceMap", values);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
        postProcess(parserContext, getBeanAssembler(element, builder), element);
    }


    protected String processHeaderName(String elementName)
    {
        String[] words = elementName.split("-");
        StringBuilder result = new StringBuilder();

        for(int index = 0; index < words.length; index++)
        {
            result.append(Character.toUpperCase(words[index].charAt(0)));
            result.append(words[index].substring(1, words[index].length()));

            if(index < (words.length - 1))
            {
                result.append("-");
            }
        }

        return result.toString();

    }

}
