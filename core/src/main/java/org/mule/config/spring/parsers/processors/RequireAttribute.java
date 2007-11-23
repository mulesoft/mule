/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.util.CoreXMLUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Throws an exception if any of the required attributes (after translation) are missing.
 * Designed to cooperates with
 * {@link org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser#addHandledException(Class)}
 */
public class RequireAttribute implements PreProcessor
{

    private Set required;

    public RequireAttribute(String required)
    {
        this(new String[]{required});
    }

    public RequireAttribute(String[] required)
    {
        this.required = new HashSet(Arrays.asList(required));
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        NamedNodeMap attributes = element.getAttributes();
        Iterator names = required.iterator();
        while (names.hasNext())
        {
            String name = (String) names.next();
            boolean found = false;
            for (int i = 0; i < attributes.getLength() && !found; i++)
            {
                String alias = CoreXMLUtils.attributeName((Attr) attributes.item(i));
                // don't translate to alias because the error message is in terms of the attributes
                // the user enters - we don't want to expose the details of translations
//                found = name.equals(config.translateName(alias));
                found = name.equals(alias);
            }
            if (!found)
            {
                throw new RequireAttributeException("Attribute " + name + " is required here.");
            }
        }
    }

    public static class RequireAttributeException extends IllegalStateException
    {

        private RequireAttributeException(String message)
        {
            super(message);
        }

    }

}