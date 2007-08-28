/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.XsltTransformerDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.routing.filters.xml.IsXmlFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.routing.outbound.RoundRobinXmlSplitter;
import org.mule.transformers.xml.DomDocumentToXml;
import org.mule.transformers.xml.JXPathExtractor;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.transformers.xml.XmlToObject;
import org.mule.util.properties.Dom4jPropertyExtractor;
import org.mule.util.properties.JDomPropertyExtractor;
import org.mule.util.properties.JXPathPropertyExtractor;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class XmlNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("jxpath-filter", new ChildDefinitionParser("filter", JXPathFilter.class));
        registerBeanDefinitionParser("is-xml-filter", new ChildDefinitionParser("filter", IsXmlFilter.class));
        registerBeanDefinitionParser("message-splitter", new RouterDefinitionParser("router", FilteringXmlMessageSplitter.class));
        registerBeanDefinitionParser("round-robin-splitter", new RouterDefinitionParser("router", RoundRobinXmlSplitter.class).addAlias("endpointFiltering", "enableEndpointFiltering"));
        registerBeanDefinitionParser("dom-to-xml", new OrphanDefinitionParser(DomDocumentToXml.class, false));
        registerBeanDefinitionParser("jxpath-extractor", new OrphanDefinitionParser(JXPathExtractor.class, false));
        registerBeanDefinitionParser("object-to-xml", new OrphanDefinitionParser(ObjectToXml.class, false));
        registerBeanDefinitionParser("xml-to-dom", new OrphanDefinitionParser(XmlToDomDocument.class, false));
        registerBeanDefinitionParser("xml-to-object", new OrphanDefinitionParser(XmlToObject.class, false));
        registerBeanDefinitionParser("xslt-transformer", new XsltTransformerDefinitionParser());
        registerBeanDefinitionParser("jxpath-property-extractor", new ChildDefinitionParser("propertyExtractor", JXPathPropertyExtractor.class));
        registerBeanDefinitionParser("dom4j-property-extractor", new ChildDefinitionParser("propertyExtractor", Dom4jPropertyExtractor.class));
        registerBeanDefinitionParser("jdom-property-extractor", new ChildDefinitionParser("propertyExtractor", JDomPropertyExtractor.class));
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));
        registerBeanDefinitionParser("context-property", new ChildMapEntryDefinitionParser("contextProperties", "key", "value"));
    }

}

