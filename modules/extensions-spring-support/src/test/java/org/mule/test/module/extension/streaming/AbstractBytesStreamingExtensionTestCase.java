/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertType;

import org.mule.metadata.api.model.UnionType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(BYTES_STREAMING)
public abstract class AbstractBytesStreamingExtensionTestCase extends AbstractStreamingExtensionTestCase {

  private static final String BARGAIN_SPELL = "dormammu i've come to bargain";
  private static final String TOO_BIG = "Too big!";
  private static final int TIMEOUT = 2000;
  private static final int DELAY = 200;
  private static List<String> CASTED_SPELLS = new LinkedList<>();

  public static void addSpell(String spell) {
    synchronized (CASTED_SPELLS) {
      CASTED_SPELLS.add(spell);
    }
  }

  @Inject
  @Named("bytesCaster")
  private Flow bytesCaster;

  @Inject
  @Named("sourceWithExceededBuffer")
  private Flow sourceWithExceededBuffer;

  @Inject
  @Named("bytesCasterInTx")
  private Flow bytesCasterInTx;

  @Inject
  @Named("bytesCasterWithoutStreaming")
  private Flow bytesCasterWithoutStreaming;

  @Inject
  @Named("toNonRepeatableStream")
  private Flow toNonRepeatableStream;

  @Inject
  private StreamingManager streamingManager;

  @Rule
  public SystemProperty configName;

  public AbstractBytesStreamingExtensionTestCase(String configName) {
    this.configName = new SystemProperty("configName", configName);
  }

  private String data = randomAlphabetic(2048);

  @Override
  protected String getConfigFile() {
    return "bytes-streaming-extension-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    setDisposeContextPerClass(true);
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    super.doTearDownAfterMuleContextDispose();
    CASTED_SPELLS.clear();
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
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
    Object value = flowRunner("toSimpleStream").withPayload(data).keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(InputStream.class)));
    assertThat(IOUtils.toString((InputStream) value), is(data));
  }

  @Test(expected = Exception.class)
  @Description("If the flow fails, all cursors should be closed")
  public void allStreamsClosedInCaseOfException() throws Exception {
    flowRunner("crashCar").withPayload(data).run();
  }

  @Test(expected = Exception.class)
  @Description("If the flow fails, all non repeatable streams should be closed")
  public void nonRepeatableStreamsClosedInCaseOfException() throws Exception {
    flowRunner("nonRepeatableCrashCar").withPayload(data).run();
  }

  @Test
  @Description("If the flow fails, all non repeatable streams should be closed")
  public void allStreamsClosedInCaseOfHandledException() throws Exception {
    flowRunner("handledCrashCar").withPayload(data).run();
  }

  @Test
  @Description("If the flow fails, all non repeatable streams should be closed")
  public void nonRepeatableStreamsClosedInCaseOfHandledException() throws Exception {
    flowRunner("nonRepeatableHandledCrashCar").withPayload(data).run();
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
    CoreEvent result = flowRunner("rewind").withPayload(data).run();
    Message firstRead = (Message) result.getVariables().get("firstRead").getValue();
    Message secondRead = (Message) result.getVariables().get("secondRead").getValue();

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
    data = randomAlphabetic(KB.toBytes(60));
    Object value = flowRunner("bufferExceeded").withPayload(data).run().getMessage().getPayload().getValue();
    assertThat(value, is(TOO_BIG));
  }

  private void doSeek(String flowName) throws Exception {
    final int position = 10;
    CoreEvent result = flowRunner(flowName)
        .withPayload(data)
        .withVariable("position", position)
        .run();

    Object value = result.getMessage().getPayload().getValue();
    assertThat(value, is(data.substring(position)));
  }

  @Test
  @Description("A source generates a cursor stream")
  public void sourceStreaming() throws Exception {
    startSourceAndListenSpell(bytesCaster, bargainPredicate());
  }

  @Test
  @Description("When the max buffer size is exceeded on a stream generated in a source, the correct type of error is mapped")
  public void sourceThrowsBufferSizeExceededError() throws Exception {
    startSourceAndListenSpell(sourceWithExceededBuffer, s -> TOO_BIG.equals(s));
  }

  @Test
  @Description("A source generates a cursor in a transaction")
  public void sourceStreamingInTx() throws Exception {
    startSourceAndListenSpell(bytesCasterInTx, bargainPredicate());
  }

  @Test
  @Description("A source is configured not to stream")
  public void sourceWithoutStreaming() throws Exception {
    startSourceAndListenSpell(bytesCasterWithoutStreaming, bargainPredicate());
  }

  @Test
  @Description("New cursor is open for Object parameter which receives a CursorProvider")
  public void resolveCursorsFromObjectParams() throws Exception {
    CursorStreamProviderFactory factory = streamingManager
        .forBytes()
        .getInMemoryCursorProviderFactory(InMemoryCursorStreamConfig.getDefault());

    CursorStreamProvider provider = (CursorStreamProvider) flowRunner("objectToStream")
        .keepStreamsOpen()
        .withPayload(factory.of(testEvent().getContext(), new ByteArrayInputStream(data.getBytes())))
        .run().getMessage().getPayload().getValue();

    byte[] bytes = muleContext.getObjectSerializer().getInternalProtocol().serialize(provider);
    bytes = muleContext.getObjectSerializer().getInternalProtocol().deserialize(bytes);
    assertThat(new String(bytes, Charset.defaultCharset()), equalTo(data));
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

  @Test
  @Description("A non repeatable stream is closed automatically when flow completes")
  public void nonRepeatableStreamIsAutomaticallyClosed() throws Exception {
    InputStream stream = (InputStream) flowRunner("toNonRepeatableStream")
        .withPayload(data).run().getMessage().getPayload().getValue();
    // Stream will close on terminate but flowRunner will be done on response
    new PollingProber(TIMEOUT, DELAY).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(stream.read(), is(-1));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Stream was not automatically closed.";
      }
    });

  }

  @Test
  @Description("A non repeatable stream is not automatically closed when event completes async")
  public void nonRepeatableStreamClosesAsync() throws Exception {
    InputStream stream = (InputStream) flowRunner("toNonRepeatableStream").keepStreamsOpen()
        .withPayload(data).run().getMessage().getPayload().getValue();

    byte[] bytes = new byte[data.length()];
    assertThat(stream.read(bytes), is(data.length()));
    assertThat(stream.read(), is(-1));
    assertThat(new String(bytes), equalTo(data));
  }

  @Test
  @Description("Streaming operation has a streaming strategy parameter")
  public void streamingStrategyParameterInOperation() throws Exception {
    ParameterModel streamingParameter =
        getStreamingStrategyParameterModel(() -> getConfigurationModel().getOperationModel("toStream").get());
    assertStreamingStrategyParameter(streamingParameter);
  }

  @Test
  @Description("Streaming source has a streaming strategy parameter")
  public void streamingStrategyParameterInSource() throws Exception {
    ParameterModel streamingParameter =
        getStreamingStrategyParameterModel(() -> getConfigurationModel().getSourceModel("bytes-caster").get());
    assertStreamingStrategyParameter(streamingParameter);
  }

  @Test
  @Description("Call operation multiple times in the flow")
  public void operationCalledAndOutputConsumedMultipleTimes() throws Exception {
    Object value =
        flowRunner("toStreamMultipleTimes").withPayload(data).keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(InputStream.class)));
    assertThat(IOUtils.toString((InputStream) value), is(data));
  }

  private ParameterModel getStreamingStrategyParameterModel(Supplier<ParameterizedModel> model) {
    return model.get().getAllParameterModels().stream()
        .filter(p -> p.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME))
        .findFirst()
        .get();
  }

  private ConfigurationModel getConfigurationModel() {
    return getExtensionModel("Marvel")
        .map(extension -> extension.getConfigurationModel("dr-strange").get())
        .get();
  }

  private void assertStreamingStrategyParameter(ParameterModel parameter) {
    assertType(parameter.getType(), Object.class, UnionType.class);
  }

  private void startSourceAndListenSpell(Flow flow, Predicate<String> predicate) throws Exception {
    if (!flow.getLifecycleState().isStarted()) {
      flow.start();
    }

    new PollingProber(4000, 100).check(new JUnitLambdaProbe(() -> {
      synchronized (CASTED_SPELLS) {
        return CASTED_SPELLS.stream().anyMatch(predicate);
      }
    }));
  }

  private Predicate<String> bargainPredicate() {
    return s -> s.equals(BARGAIN_SPELL);
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
