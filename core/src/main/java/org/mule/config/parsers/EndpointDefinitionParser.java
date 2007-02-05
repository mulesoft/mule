/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.EndpointException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * TODO
 */
public class EndpointDefinitionParser extends AbstractChildBeanDefinitionParser
{
    public static final String ADDRESS_ATTRIBUTE = "address";

    public EndpointDefinitionParser()
    {
        registerAttributeMapping("transformers", "transformer");
        registerAttributeMapping("responseTransformers", "responseTransformer");
    }

    /**
     * If the endpoint element is decared in the root beens element, this will
     * return null since there is no property to be set on a parent bean. In this case the
     * endpoint is a global endpoint and can be referenced by other components
     * @param e the current Endpoint element
     * @return the parent property name or null if the endpoint is in the root beans element.
     */
    public String getPropertyName(Element e)
    {
        Element parent = (Element) e.getParentNode();
        if (parent.getNodeName().equals("beans"))
        {
            return null;
        }
        return "endpoint";
    }


    /**
     * If the endpoint element is nested inside a parent element such as a router
     * it is considered to be (potentially) a collection of endpoints and Mule will do
     * some additional wiring behind the scenes.
     * @param element the  current endpoint element
     * @return
     */
    public boolean isCollection(Element element)
    {
        //Some types take a single endpoint, other take a collection
        Element parent = (Element) element.getParentNode();
        if (parent.getNodeName().equals("beans"))
        {
            return false;
        }
        else if ("forwardingCatchAllStrategyType".equals(parent.getSchemaTypeInfo().getTypeName()))
        {
            return false;
        }
        else if ("wireTapType".equals(parent.getSchemaTypeInfo().getTypeName()))
        {
            return false;
        }
        return true;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        registry = parserContext.getRegistry();
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            String name = attribute.getName();
            if (ATTRIBUTE_IDREF.equals(name))
            {
                continue;
            }
            else if (ADDRESS_ATTRIBUTE.equals(name))
            {
                String address = element.getAttribute("address");
                try
                {
                    builder.addPropertyValue("endpointURI", new MuleEndpointURI(address));
                }
                catch (EndpointException e)
                {
                    throw new BeanCreationException(new Message(Messages.ENPOINT_X_IS_MALFORMED, address).getMessage(), e);
                }
                continue;
            }

            String propertyName = extractPropertyName(name);
            Assert.state(org.springframework.util.StringUtils.hasText(propertyName),
                    "Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
            builder.addPropertyValue(propertyName, attribute.getValue());
        }
        postProcess(builder, element);
    }



    protected Class getBeanClass(Element element)
    {
        return MuleEndpoint.class;
    }
}
