/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.transformers.xml.XsltTransformer;
import org.mule.xml.util.XMLUtils;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XsltTransformerDefinitionParser extends MuleOrphanDefinitionParser
{
    public XsltTransformerDefinitionParser()
    {
        super(XsltTransformer.class, false);
        addAlias("transformerFactoryClass", "xslTransformerFactory");
    }

}
