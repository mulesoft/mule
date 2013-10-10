/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.module.xml.filters.JaxenFilter;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.module.xml.filters.XPathFilter;
import org.mule.module.xml.routing.FilterBasedXmlMessageSplitter;
import org.mule.module.xml.routing.XmlMessageSplitter;
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

    public void init()
    {
        //Filters
        registerBeanDefinitionParser("jxpath-filter", new FilterDefinitionParser(JXPathFilter.class));
        registerBeanDefinitionParser("jaxen-filter", new FilterDefinitionParser(JaxenFilter.class));
        registerBeanDefinitionParser("is-xml-filter", new FilterDefinitionParser(IsXmlFilter.class));
        registerBeanDefinitionParser("xpath-filter", new FilterDefinitionParser(XPathFilter.class));
        registerBeanDefinitionParser("schema-validation-filter", new FilterDefinitionParser(SchemaValidationFilter.class));

        //Routers TODO: remove
        registerBeanDefinitionParser("round-robin-splitter", new RouterDefinitionParser(XmlMessageSplitter.class));
        registerBeanDefinitionParser("filter-based-splitter", new RouterDefinitionParser(FilterBasedXmlMessageSplitter.class));

        //Simple Xml transformers
        registerBeanDefinitionParser("dom-to-xml-transformer", new MessageProcessorDefinitionParser(DomDocumentToXml.class));
        registerBeanDefinitionParser("dom-to-output-handler-transformer", new MessageProcessorDefinitionParser(XmlToOutputHandler.class));
        registerBeanDefinitionParser("jxpath-extractor-transformer", new MessageProcessorDefinitionParser(JXPathExtractor.class));
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

