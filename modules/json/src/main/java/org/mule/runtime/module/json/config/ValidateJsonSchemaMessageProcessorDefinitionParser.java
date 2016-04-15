/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.json.validation.ValidateJsonSchemaMessageProcessor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class ValidateJsonSchemaMessageProcessorDefinitionParser extends ChildDefinitionParser
{

    private static final String SCHEMA_REDIRECTS_PROPERTY_NAME = "schemaRedirects";
    private static final String SCHEMA_REDIRECTS_ELEMENT_NAME = "schema-redirects";
    private static final String SCHEMA_REDIRECT_ELEMENT_NAME = "schema-redirect";
    private static final String FROM = "from";
    private static final String TO = "to";

    public ValidateJsonSchemaMessageProcessorDefinitionParser()
    {
        super("messageProcessor", ValidateJsonSchemaMessageProcessor.class);
        addIgnored(SCHEMA_REDIRECTS_PROPERTY_NAME);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        parseRedirects(element, builder);
    }

    private void parseRedirects(Element element, BeanDefinitionBuilder builder)
    {
        Element redirectsElement = DomUtils.getChildElementByTagName(element, SCHEMA_REDIRECTS_ELEMENT_NAME);
        if (redirectsElement != null)
        {
            Map<String, String> redirectMap = new HashMap<>();
            for (Element redirect : DomUtils.getChildElementsByTagName(redirectsElement, SCHEMA_REDIRECT_ELEMENT_NAME))
            {
                redirectMap.put(redirect.getAttribute(FROM), redirect.getAttribute(TO));
            }

            builder.addPropertyValue(SCHEMA_REDIRECTS_PROPERTY_NAME, redirectMap);
        }
    }
}
