/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.stax;

import java.util.List;

import javanet.staxutils.events.EndElementEvent;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Namespace;

public class EndElementEventX extends EndElementEvent
{
    private List<Namespace> namespaces2;

    public EndElementEventX(QName name, List<Namespace> namespaces)
    {
        super(name, namespaces.iterator());
        this.namespaces2 = namespaces;
    }

    public List<Namespace> getNamespaceList()
    {
        return namespaces2;
    }
}
