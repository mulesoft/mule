/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.util.XMLTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class IsXmlFilterTestCase extends AbstractMuleTestCase {

  private IsXmlFilter filter;

  @Before
  public void setUp() {
    filter = new IsXmlFilter();
  }

  @Test
  public void testFilterFalse() throws Exception {
    assertFalse(filter.accept(InternalMessage.builder().payload("This is definitely not XML.").build(),
                              mock(Event.Builder.class)));
  }

  @Test
  public void testFilterFalse2() throws Exception {
    assertFalse(filter
        .accept(InternalMessage.builder().payload("<line>This is almost XML</line><line>This is almost XML</line>").build(),
                mock(Event.Builder.class)));
  }

  @Test
  public void testFilterTrue() throws Exception {
    assertTrue(filter.accept(InternalMessage.builder().payload("<msg attrib=\"att1\">This is some nice XML!</msg>").build(),
                             mock(Event.Builder.class)));
  }

  @Test
  public void testFilterBytes() throws Exception {
    byte[] bytes = "<msg attrib=\"att1\">This is some nice XML!</msg>".getBytes();
    assertTrue(filter.accept(InternalMessage.builder().payload(bytes).build(), mock(Event.Builder.class)));
  }

  @Test
  public void testFilterNull() throws Exception {
    assertFalse(filter.accept(InternalMessage.builder().nullPayload().build(), mock(Event.Builder.class)));
  }

  @Test
  public void testFilterLargeXml() throws Exception {
    InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", getClass());
    assertNotNull("Test resource not found.", is);
    final String xml = IOUtils.toString(is);
    assertTrue(filter.accept(InternalMessage.builder().payload(xml).build(), mock(Event.Builder.class)));
  }

  @Test
  public void testFilterLargeXmlCompliantHtml() throws Exception {
    InputStream is = IOUtils.getResourceAsStream("cdcatalog.html", getClass());
    assertNotNull("Test resource not found.", is);
    final String html = IOUtils.toString(is);
    assertTrue(filter.accept(InternalMessage.builder().payload(html).build(), mock(Event.Builder.class)));
  }

  @Test
  public void testFilterXmlMessageVariants() throws Exception {
    List<?> list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
    for (Object message : list) {
      assertTrue(filter.accept(InternalMessage.builder().payload(message).build(), mock(Event.Builder.class)));
    }
  }
}
