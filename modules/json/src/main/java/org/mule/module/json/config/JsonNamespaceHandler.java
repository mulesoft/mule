/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.config;

import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.module.json.transformers.JsonToXml;
import org.mule.module.json.transformers.ObjectToJson;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling elements defined in META-INF/mule-json.xsd
 */
public class JsonNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("is-json-filter", new FilterDefinitionParser(IsJsonFilter.class));
        registerBeanDefinitionParser("object-to-json-transformer", new TransformerDefinitionParser(ObjectToJson.class));
        registerBeanDefinitionParser("json-to-object-transformer", new TransformerDefinitionParser(JsonToObject.class));
        registerBeanDefinitionParser("json-to-xml-transformer", new TransformerDefinitionParser(JsonToXml.class));
    }
}