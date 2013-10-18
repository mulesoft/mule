/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class CheckDatabaseOrUrl implements PreProcessor
{
    @Override
    public void preProcess(PropertyConfiguration config, Element element)
    {
        boolean urlAttributePresent = false;
        boolean databaseAttributePresent = false;

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String attributeName = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            if (StringUtils.equalsIgnoreCase(attributeName, "url"))
            {
                urlAttributePresent = true;
            }
            else if (StringUtils.equalsIgnoreCase(attributeName, "database"))
            {
                databaseAttributePresent = true;
            }
        }

        if (urlAttributePresent && databaseAttributePresent)
        {
            throw new IllegalStateException("Either \"url\" or \"database\" can be configured, not both");
        }
    }
}
