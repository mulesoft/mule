/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        StringBuffer result = new StringBuffer();

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
