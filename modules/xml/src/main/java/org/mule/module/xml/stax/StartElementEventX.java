/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.stax;

import java.util.List;

import javanet.staxutils.events.StartElementEvent;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;

public class StartElementEventX extends StartElementEvent
{

    private final List attributes2;
    private final List namespaces2;

    public StartElementEventX(QName name,
                              List attributes,
                              List namespaces,
                              NamespaceContext namespaceCtx,
                              Location location,
                              QName schemaType)
    {
        super(name, attributes.iterator(), namespaces.iterator(), namespaceCtx, location, schemaType);
        attributes2 = attributes;
        namespaces2 = namespaces;
    }

    public List getAttributeList()
    {
        return attributes2;
    }

    public List getNamespaceList()
    {
        return namespaces2;
    }
    
    
}


