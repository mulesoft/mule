/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

}
