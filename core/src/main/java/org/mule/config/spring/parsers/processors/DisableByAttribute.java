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
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

/**
 * Throws an exception if any of the disallowed attributes (after translation) is present.
 * Designed to cooperates with
 * {@link org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser#addHandledException(Class)}
 */
public class DisableByAttribute implements PreProcessor
{

    private Set disallowed;

    public DisableByAttribute(String[] disallowed)
    {
        this.disallowed = new HashSet(Arrays.asList(disallowed));
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String alias = CoreXMLUtils.attributeName((Attr) attributes.item(i));
            String name = config.translateName(alias);
            if (disallowed.contains(name))
            {
                throw new DisableByAttributeException("Attribute " + alias + " is not allowed here.");
            }
        }
    }

    public static class DisableByAttributeException extends IllegalStateException
    {

        private DisableByAttributeException(String message)
        {
            super(message);
        }

    }

}
