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
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.ValueHolder;
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

  protected static List<MuleMessage> receivedMessages;


  protected static final String NAME = "Messi";
  protected static final int AGE = 10;
  protected TestPojo testPojo;
  protected byte[] testByteArray;


  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public ExpectedException expectedException = none();

  protected void assertPojo(MuleMessage message, TestPojo expectedContent) throws Exception {
    if (message.getPayload() == null) {
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

  // TODO(gfernandes) MULE-10117 remove this when support for accessing resources is added to runner
  public static class OnIncomingConnectionBean implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      MuleMessage originalMessage = eventContext.getEvent().getMessage();
      MuleMessage message = MuleMessage.builder().payload(originalMessage.getPayload())
          .mediaType(originalMessage.getDataType().getMediaType()).attributes(originalMessage.getAttributes()).build();
      receivedMessages.add(message);

      return eventContext.getEvent();
    }
  }

  protected void assertEvent(MuleMessage message, Object expectedContent) throws Exception {
    String payload = IOUtils.toString((InputStream) message.getPayload());
    assertEquals(expectedContent, payload);
  }

  protected Object deserializeMessage(MuleMessage message) throws Exception {
    return muleContext.getObjectSerializer().deserialize(IOUtils.toByteArray((InputStream) message.getPayload()));
  }

  protected MuleMessage receiveConnection() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    ValueHolder<MuleMessage> messageHolder = new ValueHolder<>();
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

  protected void assertByteArray(MuleMessage message, byte[] testByteArray) throws IOException {
    ByteArrayInputStream expectedByteArray = new ByteArrayInputStream(testByteArray);
    DataInputStream expectedData = new DataInputStream(expectedByteArray);

    // received byte array
    byte[] bytesReceived = IOUtils.toByteArray((InputStream) message.getPayload());

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesReceived);
    DataInputStream dataIn = new DataInputStream(bytesIn);

    assertEquals(expectedData.readFloat(), dataIn.readFloat(), 0.1f);
    assertEquals(expectedData.readFloat(), dataIn.readFloat(), 0.1f);
  }


}
