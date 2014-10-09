/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.module.json.transformers.JsonSchemaValidationFilter;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.module.json.transformers.JsonToXml;
import org.mule.module.json.transformers.JsonXsltTransformer;
import org.mule.module.json.transformers.ObjectToJson;
import org.mule.module.json.transformers.XmlToJson;

/**
 * Registers a Bean Definition Parser for handling elements defined in META-INF/mule-json.xsd
 */
public class JsonNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
    public void init()
    {
        registerBeanDefinitionParser("is-json-filter", new FilterDefinitionParser(IsJsonFilter.class));
        registerBeanDefinitionParser("object-to-json-transformer", new MessageProcessorDefinitionParser(ObjectToJson.class));
        registerBeanDefinitionParser("json-to-object-transformer", new MessageProcessorDefinitionParser(JsonToObject.class));
        registerBeanDefinitionParser("serialization-mixin", new ChildMapEntryDefinitionParser("serializationMixins", "targetClass", "mixinClass"));
        registerBeanDefinitionParser("deserialization-mixin", new ChildMapEntryDefinitionParser("deserializationMixins", "targetClass", "mixinClass"));
        registerBeanDefinitionParser("mixin", new ChildMapEntryDefinitionParser("mixins", "targetClass", "mixinClass"));
        registerBeanDefinitionParser("mapper", new OrphanDefinitionParser(MapperFactoryBean.class, true));
        registerBeanDefinitionParser("json-to-xml-transformer", new MessageProcessorDefinitionParser(JsonToXml.class));
        registerBeanDefinitionParser("xml-to-json-transformer", new MessageProcessorDefinitionParser(XmlToJson.class));
        registerBeanDefinitionParser("json-xslt-transformer", new MessageProcessorDefinitionParser(JsonXsltTransformer.class));
        registerDeprecatedBeanDefinitionParser("json-schema-validation-filter", new FilterDefinitionParser(JsonSchemaValidationFilter.class), "Use validate-schema instead");
        registerBeanDefinitionParser("validate-schema", new ValidateJsonSchemaMessageProcessorDefinitionParser());
        registerIgnoredElement("schema-redirects");
        registerIgnoredElement("schema-redirect");
    }
}
