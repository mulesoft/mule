/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.module.xml.filters.JaxenFilter;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.module.xml.filters.XPathFilter;
import org.mule.module.xml.transformer.DomDocumentToXml;
import org.mule.module.xml.transformer.JXPathExtractor;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XPathExtractor;
import org.mule.module.xml.transformer.XQueryTransformer;
import org.mule.module.xml.transformer.XmlPrettyPrinter;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.module.xml.transformer.XmlToOutputHandler;
import org.mule.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;

public class XmlNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        //Deprecated
        registerDeprecatedBeanDefinitionParser("jxpath-filter",new FilterDefinitionParser(JXPathFilter.class), "Use an expression-filter for filtering based in a Java Object or the xpath-filter in the case of an XML document");
        registerDeprecatedBeanDefinitionParser("jaxen-filter", new FilterDefinitionParser(JaxenFilter.class), "Use xpath-filter instead");
        registerDeprecatedBeanDefinitionParser("jxpath-extractor-transformer", new MessageProcessorDefinitionParser(JXPathExtractor.class), "Use MEL for extracting information out of a Java Object or the xpath3() MEL function in the case of an XML document");

        //Filters

        registerBeanDefinitionParser("is-xml-filter", new FilterDefinitionParser(IsXmlFilter.class));
        registerBeanDefinitionParser("xpath-filter", new FilterDefinitionParser(XPathFilter.class));
        registerBeanDefinitionParser("schema-validation-filter", new FilterDefinitionParser(SchemaValidationFilter.class));

        //Simple Xml transformers
        registerBeanDefinitionParser("dom-to-xml-transformer", new MessageProcessorDefinitionParser(DomDocumentToXml.class));
        registerBeanDefinitionParser("dom-to-output-handler-transformer", new MessageProcessorDefinitionParser(XmlToOutputHandler.class));


        registerBeanDefinitionParser("xml-to-dom-transformer", new MessageProcessorDefinitionParser(XmlToDomDocument.class));
        registerBeanDefinitionParser("xml-prettyprinter-transformer", new MessageProcessorDefinitionParser(XmlPrettyPrinter.class));
        registerBeanDefinitionParser("xpath-extractor-transformer", new MessageProcessorDefinitionParser(XPathExtractor.class));

        //JAXB
        registerBeanDefinitionParser("jaxb-object-to-xml-transformer", new MessageProcessorDefinitionParser(JAXBMarshallerTransformer.class));
        registerBeanDefinitionParser("jaxb-xml-to-object-transformer", new MessageProcessorDefinitionParser(JAXBUnmarshallerTransformer.class));
        registerBeanDefinitionParser("jaxb-context", new OrphanDefinitionParser(JaxbContextFactoryBean.class, true));

        //XStream
        registerBeanDefinitionParser("object-to-xml-transformer", new MessageProcessorDefinitionParser(ObjectToXml.class));
        registerBeanDefinitionParser("xml-to-object-transformer", new MessageProcessorDefinitionParser(XmlToObject.class));
        registerBeanDefinitionParser("alias", new ChildMapEntryDefinitionParser("aliases", "name", "class"));
        registerBeanDefinitionParser("converter", new ChildListEntryDefinitionParser("converters", "class"));

        //Namespaces
        registerBeanDefinitionParser("namespace-manager", new NamespaceManagerDefinitionParser());
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));

        //XSLT
        registerBeanDefinitionParser("xslt-transformer", new XsltTransformerDefinitionParser());
        registerBeanDefinitionParser("xslt-text", new XsltTextDefinitionParser("xslt", String.class));

        //XQuery
        registerBeanDefinitionParser("xquery-transformer", new MessageProcessorDefinitionParser(XQueryTransformer.class));
        registerBeanDefinitionParser("xquery-text", new TextDefinitionParser("xquery", true));

        //Used by XQuery and XSLT
        registerBeanDefinitionParser("context-property", new ChildMapEntryDefinitionParser("contextProperties", "key", "value"));
    }

}

