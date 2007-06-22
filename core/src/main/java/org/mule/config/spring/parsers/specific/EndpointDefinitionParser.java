/*
 * $Id:EndpointDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * The parser used for processing <code>&lt;endpoint&gt;</code> elements in Mule Xml configuration.
 * This parser is a child bean definition parser which means the endpoint created is always set on the 
 * parent object, unless the parent elemet is the root element <code>&lt;beans&gt;</code> in which 
 * case the Endpoint becomes a <em>global Endpoint</em> and is available via the registry.
 */
public class EndpointDefinitionParser extends AbstractChildDefinitionParser
{
    public static final String ADDRESS_ATTRIBUTE = "address";

    public EndpointDefinitionParser()
    {
        addAlias("address", "endpointURI");
        addAlias("transformers", "transformer");
        addAlias("responseTransformers", "responseTransformer");
        addMapping("createConnector", "GET_OR_CREATE=0,ALWAYS_CREATE=1,NEVER_CREATE=2");
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

    //@Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        //Check to see if this is a global endpoint
        Element parent = (Element) element.getParentNode();
        if (parent.getNodeName().equals("beans"))
        {
            builder.addPropertyValue("type", UMOImmutableEndpoint.ENDPOINT_TYPE_GLOBAL);
        }

        //Register non-descriptive dependencies i.e. string values for objects listed in the container
        if(StringUtils.isNotBlank(element.getAttribute("connector")))
        {
            builder.addDependsOn(element.getAttribute("connector"));
        }
        processTransformerDependencies(builder, element, "transformers");
        processTransformerDependencies(builder, element, "responseTransformers");

        super.parseChild(element, parserContext, builder);
    }

    protected void processTransformerDependencies(BeanDefinitionBuilder builder, Element element, String attributeName)
    {
        if(StringUtils.isNotBlank(element.getAttribute(attributeName)))
        {
            String[] trans = StringUtils.split(element.getAttribute(attributeName), " ,;");
            for (int i = 0; i < trans.length; i++)
            {
                builder.addDependsOn(trans[i]);
            }
        }
    }

    protected Class getBeanClass(Element element)
    {
        return MuleEndpoint.class;
    }
}
