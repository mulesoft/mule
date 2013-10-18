/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


