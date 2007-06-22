/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.MuleProperties;
import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Sets a Map of service overrides on the parent connector
 */
public class ServiceOverridesDefinitionParser extends AbstractChildDefinitionParser
{

    protected Class getBeanClass(Element element)
    {
        return HashMap.class;
    }


    public String getPropertyName(Element e)
    {
        return "serviceOverrides";
    }


    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Map overrides = new HashMap();
        addOverride(overrides, element, "messageReceiver", MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS);
        addOverride(overrides, element, "transactedessageReceiver", MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS);
        addOverride(overrides, element, "dispatcherFactory", MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        addOverride(overrides, element, "messageAdapter", MuleProperties.CONNECTOR_MESSAGE_ADAPTER);
        addOverride(overrides, element, "streamMessageAdapter", MuleProperties.CONNECTOR_STREAM_MESSAGE_ADAPTER);
        addOverride(overrides, element, "inboundTransformer", MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
        addOverride(overrides, element, "outboundTransformer", MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
        addOverride(overrides, element, "responseTransformer", MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
        addOverride(overrides, element, "endpointBuilder", MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        addOverride(overrides, element, "serviceFinder", MuleProperties.SERVICE_FINDER);
        builder.setSource(overrides);

        getBeanAssembler(element, builder).extendTarget(getPropertyName(element), overrides, false);
    }

    protected void addOverride(Map overrides, Element e, String attributeName, String overrideName)
    {
        String value = e.getAttribute(attributeName);
        if (!StringUtils.isBlank(value))
        {
            overrides.put(overrideName, value);
        }
    }
}
