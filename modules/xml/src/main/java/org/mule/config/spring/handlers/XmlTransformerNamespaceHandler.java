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

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.XsltTransformerDefinitionParser;
import org.mule.transformers.xml.DomDocumentToXml;
import org.mule.transformers.xml.JXPathExtractor;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.transformers.xml.XmlToObject;
import org.mule.transformers.xml.XsltTransformer;

/**
 * Handles all transformers in XML config module (separate namespaces for separate component
 * types helps keep schema restricted)
 */
public class XmlTransformerNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("dom-to-xml", new OrphanDefinitionParser(DomDocumentToXml.class, false));
        registerBeanDefinitionParser("jxpath-extractor", new OrphanDefinitionParser(JXPathExtractor.class, false));
        registerBeanDefinitionParser("object-to-xml", new OrphanDefinitionParser(ObjectToXml.class, false));
        registerBeanDefinitionParser("xml-to-dom", new OrphanDefinitionParser(XmlToDomDocument.class, false));
        registerBeanDefinitionParser("xml-to-object", new OrphanDefinitionParser(XmlToObject.class, false));
        registerBeanDefinitionParser("xslt", new XsltTransformerDefinitionParser());
    }

}