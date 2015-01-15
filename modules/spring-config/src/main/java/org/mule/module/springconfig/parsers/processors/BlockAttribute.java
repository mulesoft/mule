/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.processors;

import org.mule.module.springconfig.parsers.PreProcessor;
import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.module.springconfig.util.SpringXMLUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Throws an exception if any of the disallowed attributes (after translation) is present.
 * Designed to cooperates with
 * {@link org.mule.module.springconfig.parsers.delegate.AbstractSerialDelegatingDefinitionParser#addHandledException(Class)}
 */
public class BlockAttribute implements PreProcessor
{
    private Set<String> disallowed;

    public BlockAttribute(String disallowed)
    {
        this(new String[]{ disallowed });
    }

    public BlockAttribute(String[] disallowed)
    {
        this.disallowed = new HashSet<String>(Arrays.asList(disallowed));
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            String name = config.translateName(alias);
            if (disallowed.contains(name))
            {
                throw new BlockAttributeException("Attribute " + alias + " is not allowed here.");
            }
        }
    }

    public static class BlockAttributeException extends IllegalStateException
    {
        BlockAttributeException(String message)
        {
            super(message);
        }
    }
}
