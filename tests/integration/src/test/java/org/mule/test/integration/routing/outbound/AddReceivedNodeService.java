/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.api.service.ServiceAware;
import org.mule.api.service.Service;
import org.mule.api.config.ConfigurationException;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * TODO
 */
public class AddReceivedNodeService implements ServiceAware
{
    private Service service;

    public void setService(Service service) throws ConfigurationException
    {
        this.service = service;
    }

    public Document addNodeTo(Document doc)
    {
        Element e = doc.getRootElement().addElement("Received");
        e.setText(service.getName());
        return doc;
    }
}
