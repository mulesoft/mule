/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EventMetaDataPropagationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/event-metadata-propagation-config-flow.xml";
  }

  @Test
  public void testEventMetaDataPropagation() throws Exception {
    flowRunner("component1").withPayload(TEST_PAYLOAD).run();
  }

  public static class DummyComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext context) throws Exception {
      if ("component1".equals(context.getFlowConstruct().getName())) {
        Map<String, Serializable> props = new HashMap<>();
        props.put("stringParam", "param1");
        props.put("objectParam", new Apple());
        props.put("doubleParam", 12345.6);
        props.put("integerParam", 12345);
        props.put("longParam", (long) 123456789);
        props.put("booleanParam", Boolean.TRUE);

        return InternalMessage.builder()
            .payload(new DefaultMultiPartPayload(Message.builder().payload(context.getMessageAsString(muleContext))
                .attributes(BODY_ATTRIBUTES).build(),
                                                 Message.builder().nullPayload().mediaType(MediaType.TEXT)
                                                     .attributes(new PartAttributes("test1")).build()))
            .outboundProperties(props).build();
      } else {
        InternalMessage msg = (InternalMessage) context.getMessage();
        assertEquals("param1", msg.getInboundProperty("stringParam"));
        final Object o = msg.getInboundProperty("objectParam");
        assertTrue(o instanceof Apple);
        assertEquals(12345.6, 12345.6, msg.<Double>getInboundProperty("doubleParam", 0d));
        assertEquals(12345, msg.<Integer>getInboundProperty("integerParam", 0).intValue());
        assertEquals(123456789, msg.<Long>getInboundProperty("longParam", 0L).longValue());
        assertEquals(Boolean.TRUE, msg.getInboundProperty("booleanParam", Boolean.FALSE));
        assertThat(msg.getPayload().getValue(), instanceOf(DefaultMultiPartPayload.class));
        assertThat(((MultiPartPayload) msg.getPayload().getValue()).getPart("test1"), not(nullValue()));
      }
      return null;
    }
  }

  /**
   * Extend AbstractMessageAwareTransformer, even though it's deprecated, to ensure that it keeps working for compatibility with
   * older user-written transformers.
   */
  public static class DummyTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      InternalMessage msg = (InternalMessage) event.getMessage();
      assertEquals("param1", msg.getOutboundProperty("stringParam"));
      final Object o = msg.getOutboundProperty("objectParam");
      assertTrue(o instanceof Apple);
      assertEquals(12345.6, 12345.6, msg.getOutboundProperty("doubleParam", 0d));
      assertEquals(12345, msg.getOutboundProperty("integerParam", 0).intValue());
      assertEquals(123456789, msg.getOutboundProperty("longParam", 0L).longValue());
      assertEquals(Boolean.TRUE, msg.getOutboundProperty("booleanParam", Boolean.FALSE));
      return msg;
    }
  }
}
