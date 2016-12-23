/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Base class with common behaviour for all the {@link SocketsExtension} test cases
 */
public abstract class SocketExtensionTestCase extends MuleArtifactFunctionalTestCase {

  protected static final int TIMEOUT_MILLIS = 5000;
  protected static final int POLL_DELAY_MILLIS = 100;
  public static final String TEST_STRING = "This is a test string";
  public static final String RESPONSE_TEST_STRING = TEST_STRING + "_modified";

  /**
   * For tests with multiple sends
   */
  protected static final int REPETITIONS = 3;

  protected static List<Message> receivedMessages;


  protected static final String NAME = "Messi";
  protected static final int AGE = 10;
  protected TestPojo testPojo;
  protected byte[] testByteArray;


  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public ExpectedException expectedException = none();

  protected void assertPojo(Message message, TestPojo expectedContent) throws Exception {
    if (message.getPayload().getValue() == null) {
      fail("Null payload");
    }

    TestPojo pojo = (TestPojo) deserializeMessage(message);
    assertThat(pojo.getAge(), is(expectedContent.getAge()));
    assertThat(pojo.getName(), is(expectedContent.getName()));
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    receivedMessages = new CopyOnWriteArrayList<>();
    testPojo = new TestPojo();
    testPojo.setAge(AGE);
    testPojo.setName(NAME);

    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    DataOutputStream dataOut = new DataOutputStream(bytesOut);
    dataOut.writeFloat(1.0f);
    dataOut.writeFloat(2.0f);
    testByteArray = bytesOut.toByteArray();
  }

  @Override
  protected void doTearDown() throws Exception {
    receivedMessages = null;
  }

  public static class OnIncomingConnection {

    @SuppressWarnings("unused")
    public Object onCall(MessageContext messageContext) throws Exception {
      Message message = Message.builder().payload(messageContext.getPayload())
          .mediaType(messageContext.getDataType().getMediaType()).attributes(messageContext.getAttributes()).build();
      receivedMessages.add(message);

      return messageContext;
    }
  }

  protected void assertEvent(Message message, Object expectedContent) throws Exception {
    String payload = IOUtils.toString((InputStream) message.getPayload().getValue());
    assertEquals(expectedContent, payload);
  }

  protected Object deserializeMessage(Message message) throws Exception {
    return muleContext.getObjectSerializer().getExternalProtocol()
        .deserialize(IOUtils.toByteArray((InputStream) message.getPayload().getValue()));
  }

  protected Message receiveConnection() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    Reference<Message> messageHolder = new Reference<>();
    prober.check(new JUnitLambdaProbe(() -> {
      if (!receivedMessages.isEmpty()) {
        messageHolder.set(receivedMessages.remove(0));
        return true;
      }
      return false;
    }));
    return messageHolder.get();
  }

  protected void sendString(String flowName) throws Exception {
    flowRunner(flowName).withPayload(TEST_STRING).run();
    assertEvent(receiveConnection(), TEST_STRING);
  }

  protected void sendPojo(String flownName) throws Exception {
    flowRunner(flownName).withPayload(testPojo).run();
    assertPojo(receiveConnection(), testPojo);
  }

  protected void sendByteArray(String flownName) throws Exception {
    flowRunner(flownName).withPayload(testByteArray).run();
    assertByteArray(receiveConnection(), testByteArray);
  }

  protected void assertByteArray(Message message, byte[] testByteArray) throws IOException {
    ByteArrayInputStream expectedByteArray = new ByteArrayInputStream(testByteArray);
    DataInputStream expectedData = new DataInputStream(expectedByteArray);

    // received byte array
    byte[] bytesReceived = IOUtils.toByteArray((InputStream) message.getPayload().getValue());

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesReceived);
    DataInputStream dataIn = new DataInputStream(bytesIn);

    assertEquals(expectedData.readFloat(), dataIn.readFloat(), 0.1f);
    assertEquals(expectedData.readFloat(), dataIn.readFloat(), 0.1f);
  }


}
