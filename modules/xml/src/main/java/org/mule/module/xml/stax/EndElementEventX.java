/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
