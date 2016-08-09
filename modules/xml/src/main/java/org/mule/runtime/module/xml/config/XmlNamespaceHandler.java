/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;
import org.mule.runtime.module.xml.filters.IsXmlFilter;
import org.mule.runtime.module.xml.filters.SchemaValidationFilter;
import org.mule.runtime.module.xml.filters.XPathFilter;
import org.mule.runtime.module.xml.transformer.DomDocumentToXml;
import org.mule.runtime.module.xml.transformer.ObjectToXml;
import org.mule.runtime.module.xml.transformer.XPathExtractor;
import org.mule.runtime.module.xml.transformer.XQueryTransformer;
import org.mule.runtime.module.xml.transformer.XmlPrettyPrinter;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;
import org.mule.runtime.module.xml.transformer.XmlToObject;
import org.mule.runtime.module.xml.transformer.XmlToOutputHandler;
import org.mule.runtime.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.runtime.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;

public class XmlNamespaceHandler extends AbstractMuleNamespaceHandler {

  @Override
  public void init() {
    // Filters

    registerBeanDefinitionParser("is-xml-filter", new FilterDefinitionParser(IsXmlFilter.class));
    registerBeanDefinitionParser("xpath-filter", new FilterDefinitionParser(XPathFilter.class));
    registerBeanDefinitionParser("schema-validation-filter", new FilterDefinitionParser(SchemaValidationFilter.class));

    // Simple Xml transformers
    registerBeanDefinitionParser("dom-to-xml-transformer",
                                 new TransformerMessageProcessorDefinitionParser(DomDocumentToXml.class));
    registerBeanDefinitionParser("dom-to-output-handler-transformer",
                                 new TransformerMessageProcessorDefinitionParser(XmlToOutputHandler.class));


    registerBeanDefinitionParser("xml-to-dom-transformer",
                                 new TransformerMessageProcessorDefinitionParser(XmlToDomDocument.class));
    registerBeanDefinitionParser("xml-prettyprinter-transformer", new MessageProcessorDefinitionParser(XmlPrettyPrinter.class));
    registerBeanDefinitionParser("xpath-extractor-transformer",
                                 new TransformerMessageProcessorDefinitionParser(XPathExtractor.class));

    // JAXB
    registerBeanDefinitionParser("jaxb-object-to-xml-transformer",
                                 new TransformerMessageProcessorDefinitionParser(JAXBMarshallerTransformer.class));
    registerBeanDefinitionParser("jaxb-xml-to-object-transformer",
                                 new TransformerMessageProcessorDefinitionParser(JAXBUnmarshallerTransformer.class));
    registerBeanDefinitionParser("jaxb-context", new OrphanDefinitionParser(JaxbContextFactoryBean.class, true));

    // XStream
    registerBeanDefinitionParser("object-to-xml-transformer", new TransformerMessageProcessorDefinitionParser(ObjectToXml.class));
    registerBeanDefinitionParser("xml-to-object-transformer", new TransformerMessageProcessorDefinitionParser(XmlToObject.class));
    registerBeanDefinitionParser("alias", new ChildMapEntryDefinitionParser("aliases", "name", "class"));
    registerBeanDefinitionParser("converter", new ChildListEntryDefinitionParser("converters", "class"));

    // Namespaces
    registerBeanDefinitionParser("namespace-manager", new NamespaceManagerDefinitionParser());
    registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));

    // XSLT
    registerBeanDefinitionParser("xslt-transformer", new XsltTransformerDefinitionParser());
    registerBeanDefinitionParser("xslt-text", new XsltTextDefinitionParser("xslt", String.class));

    // XQuery
    registerBeanDefinitionParser("xquery-transformer", new TransformerMessageProcessorDefinitionParser(XQueryTransformer.class));
    registerBeanDefinitionParser("xquery-text", new TextDefinitionParser("xquery", true));

    // Used by XQuery and XSLT
    registerBeanDefinitionParser("context-property", new ChildMapEntryDefinitionParser("contextProperties", "key", "value"));
  }

}

