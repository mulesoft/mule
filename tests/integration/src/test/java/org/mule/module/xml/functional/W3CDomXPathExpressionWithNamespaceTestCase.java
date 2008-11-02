/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.functional;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class W3CDomXPathExpressionWithNamespaceTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public static final String MESSAGE = "<foo:endpoint xmlns:foo=\"http://foo.com\">{0}</foo:endpoint>";

    public W3CDomXPathExpressionWithNamespaceTestCase()
    {
        super(true);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/foo:endpoint");
        p.setProperty("selector.evaluator", "xpath");

        return p;
    }

    protected Object getMatchMessage() throws Exception
    {
        return documentFor("matchingEndpoint1");
    }

    protected Object getErrorMessage() throws Exception
    {
        return documentFor("missingEndpoint");
    }

    protected Document documentFor(String name) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
       
        String s = MessageFormat.format(MESSAGE, name);
        Document doc = builder.parse(new ByteArrayInputStream(s.getBytes()));
        return doc;
    }

}