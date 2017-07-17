/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.mule.runtime.core.api.Event.setCurrentEvent;

import io.qameta.allure.Issue;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.session.DefaultMuleSession;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Ignore;

public class SerializedMuleMessageTransformersTestCase extends AbstractTransformerTestCase {

  private Message testObject = null;

  @Override
  protected void doSetUp() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("object", new Apple());
    props.put("number", 1);
    props.put("string", "hello");
    testObject = InternalMessage.builder().payload("test").outboundProperties(props).build();

    setCurrentEvent(eventBuilder().message(testObject).session(new DefaultMuleSession()).build());
  }

  @Override
  protected void doTearDown() throws Exception {
    setCurrentEvent(null);
  }

  @Override
  public void testTransform() throws Exception {
    // this depends on the ordering of properties in the map.
    // because we now make a copy of maps in RequestContext this order can change
  }

  @Override
  public Transformer getTransformer() throws Exception {
    return createObject(MuleMessageToByteArray.class);
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return createObject(ByteArrayToMuleMessage.class);
  }

  @Override
  public Object getTestData() {
    // return SerializationUtils.serialize(testObject);
    return testObject;
  }

  @Override
  public Object getResultData() {
    try {
      ByteArrayOutputStream bs;
      ObjectOutputStream os;

      bs = new ByteArrayOutputStream();
      os = new ObjectOutputStream(bs);
      os.writeObject(testObject);
      os.flush();
      os.close();
      return bs.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  @Override
  public boolean compareResults(Object src, Object result) {
    if (src == null && result == null) {
      return true;
    }
    if (src == null || result == null) {
      return false;
    }
    return Arrays.equals((byte[]) src, (byte[]) result);
  }

  @Override
  @Ignore("See MULE-6046")
  @Issue("MULE-6046")
  public void testRoundTrip() throws Exception {}

  @Override
  public boolean compareRoundtripResults(Object src, Object result) {
    if (src == null && result == null) {
      return true;
    }
    if (src == null || result == null) {
      return false;
    }
    if (src instanceof InternalMessage && result instanceof InternalMessage) {
      InternalMessage sourceMuleMessage = (InternalMessage) src;
      InternalMessage resultMuleMessage = (InternalMessage) result;

      boolean payloadsAreEqual = comparePayloads(sourceMuleMessage, resultMuleMessage);
      boolean objectPropertiesAreEqual = compareObjectProperties(sourceMuleMessage, resultMuleMessage);
      boolean stringPropertiesAreEqual = compareStringProperties(sourceMuleMessage, resultMuleMessage);
      boolean intPropertiesAreEqual = compareIntProperties(sourceMuleMessage, resultMuleMessage);

      return payloadsAreEqual && objectPropertiesAreEqual && stringPropertiesAreEqual && intPropertiesAreEqual;
    } else {
      return false;
    }
  }

  private boolean comparePayloads(InternalMessage src, InternalMessage result) {
    Object sourcePayload = src.getPayload().getValue();
    Object resultPayload = result.getPayload().getValue();
    return sourcePayload.equals(resultPayload);
  }

  private boolean compareObjectProperties(InternalMessage src, InternalMessage result) {
    Object sourceObjectProperty = src.getOutboundProperty("object");
    Object resultObjectProperty = result.getOutboundProperty("object");
    return sourceObjectProperty.equals(resultObjectProperty);
  }

  private boolean compareStringProperties(InternalMessage src, InternalMessage result) {
    Object sourceStringProperty = src.getOutboundProperty("string");
    Object resultStringProperty = result.getOutboundProperty("string");
    return sourceStringProperty.equals(resultStringProperty);
  }

  private boolean compareIntProperties(InternalMessage src, InternalMessage result) {
    int sourceIntProperty = src.getOutboundProperty("number", -1);
    int resultIntProperty = result.getOutboundProperty("number", -2);
    return (sourceIntProperty == resultIntProperty);
  }

}
