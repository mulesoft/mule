/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.XsltTransformerDefinitionParser;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.routing.outbound.RoundRobinXmlSplitter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.filters.xml.IsXmlFilter;
import org.mule.transformers.xml.DomDocumentToXml;
import org.mule.transformers.xml.JXPathExtractor;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.transformers.xml.XmlToObject;

public class XmlNamespaceHandler extends MuleNamespaceHandler
{

    public XmlNamespaceHandler()
    {
        setPriority(1); // override MuleNamespaceHandler (which this extends)
    }

    public void init()
    {
        super.init();
        registerBeanDefinitionParser("xml-jxpath", new ChildDefinitionParser("filter", JXPathFilter.class));
        registerBeanDefinitionParser("is-xml", new ChildDefinitionParser("filter", IsXmlFilter.class));
        registerBeanDefinitionParser("xml-message-splitter", new RouterDefinitionParser("router", FilteringXmlMessageSplitter.class));
        registerBeanDefinitionParser("xml-round-robin-splitter", new RouterDefinitionParser("router", RoundRobinXmlSplitter.class).addAlias("endpointFiltering", "enableEndpointFiltering"));
        registerBeanDefinitionParser("dom-to-xml", new OrphanDefinitionParser(DomDocumentToXml.class, false));
        registerBeanDefinitionParser("xml-jxpath-extractor", new OrphanDefinitionParser(JXPathExtractor.class, false));
        registerBeanDefinitionParser("object-to-xml", new OrphanDefinitionParser(ObjectToXml.class, false));
        registerBeanDefinitionParser("xml-to-dom", new OrphanDefinitionParser(XmlToDomDocument.class, false));
        registerBeanDefinitionParser("xml-to-object", new OrphanDefinitionParser(XmlToObject.class, false));
        registerBeanDefinitionParser("xml-xslt", new XsltTransformerDefinitionParser());
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));
    }

}

