/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.module.xml.expression.BeanPayloadExpressionEvaluator;
import org.mule.module.xml.expression.JXPathExpressionEvaluator;
import org.mule.module.xml.expression.XPathExpressionEvaluator;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.module.xml.routing.FilteringXmlMessageSplitter;
import org.mule.module.xml.routing.RoundRobinXmlSplitter;
import org.mule.module.xml.transformer.DocumentToOutputHandler;
import org.mule.module.xml.transformer.DomDocumentToXml;
import org.mule.module.xml.transformer.JXPathExtractor;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlPrettyPrinter;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.transformer.XmlToObject;

public class XmlNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jxpath-filter", new ChildDefinitionParser("filter", JXPathFilter.class));
        registerBeanDefinitionParser("is-xml-filter", new ChildDefinitionParser("filter", IsXmlFilter.class));
        registerBeanDefinitionParser("message-splitter", new RouterDefinitionParser(FilteringXmlMessageSplitter.class));
        registerMuleBeanDefinitionParser("round-robin-splitter", new RouterDefinitionParser(RoundRobinXmlSplitter.class)).addAlias("endpointFiltering", "enableEndpointFiltering");
        registerBeanDefinitionParser("dom-to-xml-transformer", new MuleOrphanDefinitionParser(DomDocumentToXml.class, false));
        registerBeanDefinitionParser("dom-to-output-handler-transformer", new MuleOrphanDefinitionParser(DocumentToOutputHandler.class, false));
        registerBeanDefinitionParser("jxpath-extractor-transformer", new MuleOrphanDefinitionParser(JXPathExtractor.class, false));
        registerBeanDefinitionParser("object-to-xml-transformer", new MuleOrphanDefinitionParser(ObjectToXml.class, false));
        registerBeanDefinitionParser("xml-to-dom-transformer", new MuleOrphanDefinitionParser(XmlToDomDocument.class, false));
        registerBeanDefinitionParser("xml-to-object-transformer", new MuleOrphanDefinitionParser(XmlToObject.class, false));
        registerBeanDefinitionParser("xml-prettyprinter-transformer", new TransformerDefinitionParser(XmlPrettyPrinter.class));
        registerBeanDefinitionParser("xslt-transformer", new XsltTransformerDefinitionParser());
        registerBeanDefinitionParser("jxpath-property-extractor", new ChildDefinitionParser("propertyExtractor", JXPathExpressionEvaluator.class));
        registerBeanDefinitionParser("xpath-property-extractor", new ChildDefinitionParser("propertyExtractor", XPathExpressionEvaluator.class));
        registerBeanDefinitionParser("bean-property-extractor", new ChildDefinitionParser("propertyExtractor", BeanPayloadExpressionEvaluator.class));
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));
        registerBeanDefinitionParser("context-property", new ChildMapEntryDefinitionParser("contextProperties", "key", "value"));
        registerBeanDefinitionParser("xslt-text", new XsltTextDefinitionParser("xslt", String.class));
    }

}

