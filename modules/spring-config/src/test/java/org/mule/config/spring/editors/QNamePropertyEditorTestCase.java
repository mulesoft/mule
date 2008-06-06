/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.editors;

import org.mule.tck.AbstractMuleTestCase;

import javax.xml.namespace.QName;

public class QNamePropertyEditorTestCase extends AbstractMuleTestCase
{

    public void testFullQNameString()
    {
        QName name = (QName) QNamePropertyEditor.convert("qname{e:echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("e", name.getPrefix());
        assertEquals("echo", name.getLocalPart());
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
    }

    public void testFullQNameStringWithColonsInNamespace()
    {
        QName name = (QName) QNamePropertyEditor.convert("qname{e:echo:urn:muleumo:echo}");
        assertNotNull(name);
        assertEquals("e", name.getPrefix());
        assertEquals("echo", name.getLocalPart());
        assertEquals("urn:muleumo:echo", name.getNamespaceURI());
    }

    public void testNameAndNamespace()
    {
        QName name = (QName) QNamePropertyEditor.convert("qname{echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    public void testNameOnly()
    {
        QName name = (QName) QNamePropertyEditor.convert("qname{echo}");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    public void testNameOnlyWithoutBraces()
    {
        QName name = (QName) QNamePropertyEditor.convert("echo");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

}
