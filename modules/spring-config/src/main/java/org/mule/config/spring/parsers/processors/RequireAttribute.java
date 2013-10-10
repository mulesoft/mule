/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;

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
                String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
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

        public RequireAttributeException(String message)
        {
            super(message);
        }

    }

}
