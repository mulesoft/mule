/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Sets a Map of service overrides on the parent connector
 */
public class ServiceOverridesDefinitionParser extends ChildDefinitionParser
{
    public ServiceOverridesDefinitionParser()
    {
        super("serviceOverrides", HashMap.class);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Map<String, String> overrides = new HashMap<String, String>();
        addOverride(overrides, element, "messageReceiver", MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS);
        addOverride(overrides, element, "transactedMessageReceiver", MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS);
        addOverride(overrides, element, "xaTransactedMessageReceiver", MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS);
        addOverride(overrides, element, "dispatcherFactory", MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        addOverride(overrides, element, "messageFactory", MuleProperties.CONNECTOR_MESSAGE_FACTORY);
        addOverride(overrides, element, "inboundTransformer", MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
        addOverride(overrides, element, "outboundTransformer", MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
        addOverride(overrides, element, "responseTransformer", MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
        addOverride(overrides, element, "endpointBuilder", MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        addOverride(overrides, element, "serviceFinder", MuleProperties.SERVICE_FINDER);
        addOverride(overrides, element, "sessionHandler", MuleProperties.CONNECTOR_SESSION_HANDLER);
        addOverride(overrides, element, "inboundExchangePatterns", MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS);
        addOverride(overrides, element, "outboundExchangePatterns", MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS);
        addOverride(overrides, element, "defaultExchangePattern", MuleProperties.CONNECTOR_DEFAULT_EXCHANGE_PATTERN);
        builder.getRawBeanDefinition().setSource(overrides);

        getBeanAssembler(element, builder).extendTarget(getPropertyName(element), overrides, false);
    }

    protected void addOverride(Map<String, String> overrides, Element e, String attributeName, String overrideName)
    {
        String value = e.getAttribute(attributeName);
        if (!StringUtils.isBlank(value))
        {
            overrides.put(overrideName, value);
        }
    }
}
