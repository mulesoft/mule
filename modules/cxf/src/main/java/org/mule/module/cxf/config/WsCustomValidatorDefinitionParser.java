/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.config;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.cxf.support.MuleSecurityManagerValidator;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.ws.security.SecurityConstants;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WsCustomValidatorDefinitionParser extends ChildDefinitionParser
{

    public WsCustomValidatorDefinitionParser(String setterMethod)
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

        NodeList properties = element.getChildNodes();
        for(int index = 0; index < properties.getLength(); index ++)
        {
            Node property = properties.item(index);
            if(property instanceof Element)
            {
                String localName = property.getLocalName();
                String ref = ((Element)property).getAttribute("ref");
                String key = "";

                if("username-token-validator".equals(localName))
                {
                    key = SecurityConstants.USERNAME_TOKEN_VALIDATOR;
                }
                else if("saml1-token-validator".equals(localName))
                {
                    key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
                }
                else if("saml2-token-validator".equals(localName))
                {
                    key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
                }
                else if("timestamp-token-validator".equals(localName))
                {
                    key = SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR;
                }
                else if("signature-token-validator".equals(localName))
                {
                    key = SecurityConstants.SIGNATURE_TOKEN_VALIDATOR;
                }
                else if("bst-token-validator".equals(localName))
                {
                    key = SecurityConstants.BST_TOKEN_VALIDATOR;
                } 
                else
                {
                    throw new IllegalArgumentException("Illegal custom validator: " + localName);
                }

                values.put(key, new RuntimeBeanReference(ref));
            }
        }

        builder.addPropertyValue("sourceMap", values);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
        postProcess(parserContext, getBeanAssembler(element, builder), element);

        builder.getBeanDefinition().setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
    }

}
