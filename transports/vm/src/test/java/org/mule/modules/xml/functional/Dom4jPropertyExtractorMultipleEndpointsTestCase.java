/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class Dom4jPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public Dom4jPropertyExtractorMultipleEndpointsTestCase()
    {
        super(false);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.property", "${xpath:/endpoints/endpoint}");
        return p;
    }

    protected Object getMatchMessage()
    {
        Document document = DocumentHelper.createDocument();
        Element e = document.addElement("endpoints");
        e.addElement("endpoint").addText("matchingEndpoint1");
        e.addElement("endpoint").addText("matchingEndpoint2");
        return document;
    }

    protected Object getErrorMessage()
    {
        Document document = DocumentHelper.createDocument();
        document.addElement("endpoint").addText("missingEndpoint");
        return document;
    }

}