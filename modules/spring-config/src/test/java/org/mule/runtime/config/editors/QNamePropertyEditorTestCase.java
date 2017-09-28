/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.editors;

import org.mule.runtime.config.internal.editors.QNamePropertyEditor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QNamePropertyEditorTestCase extends AbstractMuleTestCase {

  @Test
  public void testFullQNameString() {
    QName name = QNamePropertyEditor.convert("qname{e:echo:http://muleumo.org/echo}");
    assertNotNull(name);
    assertEquals("e", name.getPrefix());
    assertEquals("echo", name.getLocalPart());
    assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
  }

  @Test
  public void testFullQNameStringWithColonsInNamespace() {
    QName name = QNamePropertyEditor.convert("qname{e:echo:urn:muleumo:echo}");
    assertNotNull(name);
    assertEquals("e", name.getPrefix());
    assertEquals("echo", name.getLocalPart());
    assertEquals("urn:muleumo:echo", name.getNamespaceURI());
  }

  @Test
  public void testNameAndNamespace() {
    QName name = QNamePropertyEditor.convert("qname{echo:http://muleumo.org/echo}");
    assertNotNull(name);
    assertEquals("http://muleumo.org/echo", name.getNamespaceURI());
    assertEquals("echo", name.getLocalPart());
    assertEquals("", name.getPrefix());
  }

  @Test
  public void testNameOnly() {
    QName name = QNamePropertyEditor.convert("qname{echo}");
    assertNotNull(name);
    assertEquals("", name.getNamespaceURI());
    assertEquals("echo", name.getLocalPart());
    assertEquals("", name.getPrefix());
  }

  @Test
  public void testNameOnlyWithoutBraces() {
    QName name = QNamePropertyEditor.convert("echo");
    assertNotNull(name);
    assertEquals("", name.getNamespaceURI());
    assertEquals("echo", name.getLocalPart());
    assertEquals("", name.getPrefix());
  }

}
