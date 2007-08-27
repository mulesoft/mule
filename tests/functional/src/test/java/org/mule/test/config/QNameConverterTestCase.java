/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.converters.QNameConverter;
import org.mule.tck.AbstractMuleTestCase;

import javax.xml.namespace.QName;

public class QNameConverterTestCase extends AbstractMuleTestCase
{
    protected QNameConverter converter = new QNameConverter();

    public void testFullQNameString()
    {
        QName name = (QName)converter.convert(QName.class, "qname{e:echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("e", name.getPrefix());
        assertEquals("echo", name.getLocalPart());
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
    }

    public void testNameAndNamespace()
    {
        QName name = (QName)converter.convert(QName.class, "qname{echo:http://muleumo.org/echo}");
        assertNotNull(name);
        assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    public void testNameOnly()
    {
        QName name = (QName)converter.convert(QName.class, "qname{echo}");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }

    public void testNameOnlyWithoutBraces()
    {
        QName name = (QName)converter.convert(QName.class, "echo");
        assertNotNull(name);
        assertEquals("", name.getNamespaceURI());
        assertEquals("echo", name.getLocalPart());
        assertEquals("", name.getPrefix());
    }
}
