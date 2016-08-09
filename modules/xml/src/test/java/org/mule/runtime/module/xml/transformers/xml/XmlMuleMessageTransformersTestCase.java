/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.xml.transformer.ObjectToXml;
import org.mule.runtime.module.xml.transformer.XmlToObject;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Set;

import org.junit.Test;

public class XmlMuleMessageTransformersTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testMessageSerialization() throws Exception {
    ObjectToXml t1 = createObject(ObjectToXml.class);
    t1.setAcceptMuleMessage(true);

    MuleMessage msg = MuleMessage.builder().payload("test").mediaType(MediaType.ANY.withCharset(UTF_8)).correlationId("1234")
        .addOutboundProperty("object", new Apple()).addOutboundProperty("string", "hello").build();

    String xml = (String) t1.transform(msg);
    assertNotNull(xml);

    XmlToObject t2 = createObject(XmlToObject.class);

    Object result = t2.transform(xml);
    assertNotNull(result);
    assertThat(result, instanceOf(MuleMessage.class));

    msg = (MuleMessage) result;

    assertEquals("test", getPayloadAsString(msg));
    assertEquals(new Apple(), msg.getOutboundProperty("object"));
    // with different case
    assertEquals(new Apple(), msg.getOutboundProperty("oBjeCt"));
    // Make sure we don't have the property in a different scope
    assertNull(msg.getInboundProperty("oBjeCt"));

    assertEquals("hello", msg.getOutboundProperty("string"));
    // with different case
    assertEquals("hello", msg.getOutboundProperty("String"));
    // Make sure we don't have the property in a different scope
    assertNull(msg.getInboundProperty("string"));

    // Make sure we don't have the property in a different scope
    assertNull(msg.getInboundProperty("number"));
    assertNull(msg.getOutboundProperty("number"));

    assertThat(msg.getCorrelation().getId().get(), is("1234"));
    assertThat(msg.getDataType().getMediaType().getCharset().get(), is(UTF_8));


    Set<String> outboundProps = msg.getOutboundPropertyNames();
    assertThat(outboundProps, hasSize(2));

    for (String key : outboundProps) {
      assertTrue(key.equals("number") || key.equals("string") || key.equals("object"));
      assertFalse(key.equals("NUMBER") || key.equals("STRING") || key.equals("OBJECT"));
    }
  }
}
