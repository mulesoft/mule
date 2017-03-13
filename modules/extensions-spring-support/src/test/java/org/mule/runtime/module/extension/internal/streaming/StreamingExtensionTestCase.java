/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.streaming;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.core.streaming.bytes.ByteStreamingStatistics;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.marvel.MarvelExtension;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class StreamingExtensionTestCase extends ExtensionFunctionalTestCase {

  private static final String BARGAIN_SPELL = "dormammu i've come to bargain";
  private static List<String> CASTED_SPELLS = new LinkedList<>();

  public static void addSpell(String spell) {
    synchronized (CASTED_SPELLS) {
      CASTED_SPELLS.add(spell);
    }
  }

  private String data = randomAlphabetic(2048);
  private StreamingManager streamingManager;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MarvelExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "streaming-extension-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    streamingManager = muleContext.getRegistry().lookupObject(StreamingManager.class);
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    CASTED_SPELLS.clear();
    assertAllStreamingResourcesClosed();
  }

  @Test
  public void consumeGeneratedCursorAndCloseIt() throws Exception {
    Object value = flowRunner("consumeGeneratedStream").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is(data));
  }

  @Test
  public void operationWithDisabledStreaming() throws Exception {
    Object value = flowRunner("toSimpleStream").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(InputStream.class)));
    assertThat(IOUtils.toString((InputStream) value), is(data));
  }

  @Test(expected = Exception.class)
  public void allStreamsClosedInCaseOfException() throws Exception {
    flowRunner("crashCar").withPayload(data).run();
  }

  @Test(expected = Exception.class)
  public void allStreamsClosedInCaseOfExceptionInTx() throws Exception {
    flowRunner("crashCarTx").withPayload(data).run();
  }

  @Test
  public void seek() throws Exception {
    doSeek("seekStream");
  }

  @Test
  public void rewind() throws Exception {
    Event result = flowRunner("rewind").withPayload(data).run();
    Message firstRead = (Message) result.getVariable("firstRead").getValue();
    Message secondRead = (Message) result.getVariable("secondRead").getValue();

    assertThat(firstRead.getPayload().getValue(), equalTo(data));
    assertThat(secondRead.getPayload().getValue(), equalTo(data));
  }

  @Test
  public void seekInTx() throws Exception {
    doSeek("seekStreamTx");
  }

  private void doSeek(String flowName) throws Exception {
    final int position = 10;
    Event result = flowRunner(flowName)
        .withPayload(data)
        .withVariable("position", position)
        .run();

    Object value = result.getMessage().getPayload().getValue();
    assertThat(value, is(data.substring(position)));
  }

  @Test
  public void sourceStreaming() throws Exception {
    startSourceAndListenSpell("bytesCaster");
  }

  @Test
  public void sourceStreamingInTx() throws Exception {
    startSourceAndListenSpell("bytesCasterInTx");
  }

  @Test
  public void sourceWithoutStreaming() throws Exception {
    startSourceAndListenSpell("bytesCasterWithoutStreaming");
  }

  private void startSourceAndListenSpell(String flowName) throws Exception {
    Flow flow = muleContext.getRegistry().get(flowName);
    flow.start();
    new PollingProber(4000, 100).check(new JUnitLambdaProbe(() -> {
      synchronized (CASTED_SPELLS) {
        return CASTED_SPELLS.stream().anyMatch(s -> s.equals(BARGAIN_SPELL));
      }
    }));
  }

  private void assertAllStreamingResourcesClosed() {
    ByteStreamingStatistics stats = streamingManager.forBytes().getByteStreamingStatistics();
    new PollingProber(10000, 100).check(new JUnitLambdaProbe(() -> {
      assertThat("There're still open cursor providers", stats.getOpenCursorProvidersCount(), is(0));
      assertThat("There're still open cursors", stats.getOpenCursorsCount(), is(0));
      return true;
    }));
  }
}
