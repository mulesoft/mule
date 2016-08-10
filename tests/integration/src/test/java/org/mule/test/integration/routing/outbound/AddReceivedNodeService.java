/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * TODO
 */
public class AddReceivedNodeService
{
    public Document addNodeTo(Document doc)
    {
        doc.getRootElement().addElement("Received");

        return doc;
    }

    public Element addNodeTo(Element doc)
    {
        doc.addElement("Received");

        return doc;
    }

    public org.w3c.dom.Document addNodeTo(org.w3c.dom.Document doc)
    {
        doc.appendChild(doc.createElement("Received"));
        return doc;
    }

    public org.w3c.dom.Element addNodeTo(org.w3c.dom.Element element)
    {
        element.appendChild(element.getOwnerDocument().createElement("Received"));
        return element;
    }

}
