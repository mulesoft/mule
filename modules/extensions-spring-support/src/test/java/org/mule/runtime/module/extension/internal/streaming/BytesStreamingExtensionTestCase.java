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
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.marvel.MarvelExtension;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Streaming")
@Stories("Bytes Streaming")
public class BytesStreamingExtensionTestCase extends AbstractStreamingExtensionTestCase {

  private static final String BARGAIN_SPELL = "dormammu i've come to bargain";
  private static List<String> CASTED_SPELLS = new LinkedList<>();

  public static void addSpell(String spell) {
    synchronized (CASTED_SPELLS) {
      CASTED_SPELLS.add(spell);
    }
  }

  private String data = randomAlphabetic(2048);

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MarvelExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "bytes-streaming-extension-config.xml";
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    super.doTearDownAfterMuleContextDispose();
    CASTED_SPELLS.clear();
  }

  @Test
  @Description("Fully consume a cursor stream")
  public void consumeGeneratedCursorAndCloseIt() throws Exception {
    Object value = flowRunner("consumeGeneratedStream").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is(data));
  }

  @Test
  @Description("Operation with disabled streaming")
  public void operationWithDisabledStreaming() throws Exception {
    Object value = flowRunner("toSimpleStream").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(InputStream.class)));
    assertThat(IOUtils.toString((InputStream) value), is(data));
  }

  @Test(expected = Exception.class)
  @Description("If the flow fails, all cursors should be closed")
  public void allStreamsClosedInCaseOfException() throws Exception {
    flowRunner("crashCar").withPayload(data).run();
  }

  @Test(expected = Exception.class)
  @Description("If a cursor is open in a transaction, it should be closed if the flow fails")
  public void allStreamsClosedInCaseOfExceptionInTx() throws Exception {
    flowRunner("crashCarTx").withPayload(data).run();
  }

  @Test
  @Description("Read a stream from a random position")
  public void seek() throws Exception {
    doSeek("seekStream");
  }

  @Test
  @Description("Rewing a stream and consume it twice")
  public void rewind() throws Exception {
    Event result = flowRunner("rewind").withPayload(data).run();
    Message firstRead = (Message) result.getVariable("firstRead").getValue();
    Message secondRead = (Message) result.getVariable("secondRead").getValue();

    assertThat(firstRead.getPayload().getValue(), equalTo(data));
    assertThat(secondRead.getPayload().getValue(), equalTo(data));
  }

  @Test
  @Description("Read from a random position inside a transaction")
  public void seekInTx() throws Exception {
    doSeek("seekStreamTx");
  }

  @Test
  @Description("When the max buffer size is exceeded, the correct type of error is mapped")
  public void throwsBufferSizeExceededError() throws Exception {
    Object value = flowRunner("toGreedyStream").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is("Too big!"));
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
  @Description("A source generates a cursor stream")
  public void sourceStreaming() throws Exception {
    startSourceAndListenSpell("bytesCaster");
  }

  @Test
  @Description("A source generates a cursor in a transaction")
  public void sourceStreamingInTx() throws Exception {
    startSourceAndListenSpell("bytesCasterInTx");
  }

  @Test
  @Description("A source is configured not to stream")
  public void sourceWithoutStreaming() throws Exception {
    startSourceAndListenSpell("bytesCasterWithoutStreaming");
  }

  @Test
  @Description("A stream provider is serialized as a byte[]")
  public void streamProviderSerialization() throws Exception {
    CursorStreamProvider provider = (CursorStreamProvider) flowRunner("toStream").keepStreamsOpen()
        .withPayload(data)
        .run().getMessage().getPayload().getValue();

    byte[] bytes = muleContext.getObjectSerializer().getInternalProtocol().serialize(provider);
    bytes = muleContext.getObjectSerializer().getInternalProtocol().deserialize(bytes);
    assertThat(new String(bytes, Charset.defaultCharset()), equalTo(data));
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
}
