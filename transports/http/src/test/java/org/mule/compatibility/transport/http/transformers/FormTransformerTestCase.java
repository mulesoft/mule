/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class FormTransformerTestCase extends AbstractMuleContextEndpointTestCase {

  private FormTransformer transformer;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    transformer = new FormTransformer();
    transformer.setMuleContext(muleContext);
  }

  @Test
  public void testFormTransformer() throws TransformerException {
    MuleMessage msg = MuleMessage.builder().payload("test1=value1&test2=value2&test3").build();
    Object result = transformer.transform(msg);
    assertTrue(result instanceof Map);

    Map<String, String> m = (Map<String, String>) result;
    assertEquals("value1", m.get("test1"));
    assertEquals("value2", m.get("test2"));
    assertNull(m.get("test3"));
  }

  @Test
  public void testMultipleValues() throws TransformerException {
    MuleMessage msg = MuleMessage.builder().payload("test1=value1&test1=value2").build();
    Object result = transformer.transform(msg);
    assertTrue(result instanceof Map);

    Map<String, Object> m = (Map<String, Object>) result;
    Object o = m.get("test1");
    assertTrue(o instanceof List);

    List list = (List) o;
    assertTrue(list.contains("value1"));
    assertTrue(list.contains("value2"));

  }

}
