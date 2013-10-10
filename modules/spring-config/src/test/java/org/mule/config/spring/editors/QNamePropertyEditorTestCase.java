/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.editors;

import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QNamePropertyEditorTestCase extends AbstractMuleTestCase
{

    @Test
    public void testFullQNameString()
    {
        QName name = QNamePropertyEditor.convert("qname{e:echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("e", name.getPrefix());
        assertEquals("echo", name.getLocalPart());
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
    }

    @Test
    public void testFullQNameStringWithColonsInNamespace()
    {
        QName name = QNamePropertyEditor.convert("qname{e:echo:urn:muleumo:echo}");
        assertNotNull(name);
        assertEquals("e", name.getPrefix());
        assertEquals("echo", name.getLocalPart());
        assertEquals("urn:muleumo:echo", name.getNamespaceURI());
    }

    @Test
    public void testNameAndNamespace()
    {
        QName name = QNamePropertyEditor.convert("qname{echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    @Test
    public void testNameOnly()
    {
        QName name = QNamePropertyEditor.convert("qname{echo}");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    @Test
    public void testNameOnlyWithoutBraces()
    {
        QName name = QNamePropertyEditor.convert("echo");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

}
