/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.config;

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
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling elements defined in META-INF/mule-json.xsd
 */
public class JsonNamespaceHandler extends NamespaceHandlerSupport
{
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
        registerBeanDefinitionParser("json-schema-validation-filter", new FilterDefinitionParser(JsonSchemaValidationFilter.class));
    }
}
