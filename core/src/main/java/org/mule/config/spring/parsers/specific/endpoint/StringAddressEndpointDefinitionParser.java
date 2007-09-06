/*
 * $Id:EndpointDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Extend {@link UnaddressedEndpointDefinitionParser}
 * with validation of simple string address or global endpoint reference .
 *
 * @deprecated - see
 * {@link org.mule.config.spring.parsers.specific.endpoint.OrphanEndpointDefinitionParser}
 * and
 * {@link org.mule.config.spring.parsers.specific.endpoint.ChildEndpointDefinitionParser}
 */
public class StringAddressEndpointDefinitionParser extends UnaddressedEndpointDefinitionParser
{

    public StringAddressEndpointDefinitionParser(Class endpointClass)
    {
        super(endpointClass);
    }

    //@Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        //Check to see if this is a global endpoint
        if (isGlobal(element))
        {
            // if global, cannot be a reference (afaik)
            if (null == element.getAttributeNode(ADDRESS_ATTRIBUTE))
            {
                throw new IllegalStateException("A global endpoint requires an " + ADDRESS_ATTRIBUTE + " attribute.");
            }
            if (null != element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
            {
                throw new IllegalStateException("A global endpoint cannot contain a " + ENDPOINT_REF_ATTRIBUTE +
                        " attribute.");
            }
        }
        else
        {
            // must be reference *or* have an address
            if (null == element.getAttributeNode(ADDRESS_ATTRIBUTE))
            {
                if (null == element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
                {
                    throw new IllegalStateException("An endpoint requires either an " + ADDRESS_ATTRIBUTE + " or a " +
                            ENDPOINT_REF_ATTRIBUTE + " attribute.");
                }
            }
            else
            {
                if (null != element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
                {
                    throw new IllegalStateException("The " + ADDRESS_ATTRIBUTE + " and " + ENDPOINT_REF_ATTRIBUTE +
                            " attributes are mutually exclusive.");
                }
            }
        }
        
        // Not sure if this is required. Adding for now for backwards compatability
        if (element.getParentNode().getNodeName().equals("chaining-router")
            || element.getParentNode().getNodeName().equals("exception-based-router"))
        {
            builder.addPropertyValue("remoteSync", Boolean.TRUE);
        }

        super.parseChild(element, parserContext, builder);
    }

}