/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.internal.util.message.MessageUtils.decorateInputOperation;
import static org.mule.runtime.core.internal.util.message.MessageUtils.decorateOutputOperation;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.core.internal.streaming.bytes.ByteArrayCursorStream;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.streaming.PagingProvider;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsTestCase extends AbstractPayloadStatisticsTestCase {

  @Rule
  public SystemProperty muleEnableStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Test
  public void decorateInputStreamNoLocation() throws IOException {
    final InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    final InputStream decorator =
        decoratorFactory.componentDecoratorFactory(componentNoLocation).decorateInput(decorated, CORR_ID);

    assertThat(decorated, sameInstance(decorator));
  }

  @Test
  public void decorateInputStreamDisabled() throws IOException {
    getStatistics().setEnabled(false);

    final InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1).decorateInput(decorated, CORR_ID);

    assertThat(decorated, sameInstance(decorator));

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read();

    verifyNoStatistics(statistics);
  }

  @Test
  public void verifyEnablingAndDisablingStats() throws IOException {
    getStatistics().setEnabled(false);
    consumeInputStream();

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    verifyNoStatistics(statistics);

    getStatistics().enablePayloadStatistics(true);

    consumeInputStream();

    // verify stats change
    assertThat(statistics.getInputByteCount(), is(1L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));

    getStatistics().enablePayloadStatistics(false);
    consumeInputStream();

    // verify stats do not change
    assertThat(statistics.getInputByteCount(), is(1L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputStreamNotMixesComponents() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);
    decoratorFactory.componentDecoratorFactory(component2);

    final PayloadStatistics statistics2 =
        getStatistics().getPayloadStatistics(component2.getLocation().getLocation());

    decorator.read();

    assertThat(statistics2.getInputByteCount(), is(0L));
    assertThat(statistics2.getInputObjectCount(), is(0L));
    assertThat(statistics2.getOutputByteCount(), is(0L));
    assertThat(statistics2.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputStreamSingle() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read();
    assertThat(statistics.getInputByteCount(), is(1L));

    decorator.read();
    assertThat(statistics.getInputByteCount(), is(2L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputStreamArray() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read(new byte[5]);
    assertThat(statistics.getInputByteCount(), is(5L));

    decorator.read(new byte[10]);
    assertThat(statistics.getInputByteCount(), is(11L));

    decorator.read(new byte[5]);
    assertThat(statistics.getInputByteCount(), is(11L));
    assertThat(decorator.read(), is(-1));
    assertThat(statistics.getInputByteCount(), is(11L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputStreamArrayBoundaries() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read(new byte[10], 2, 5);
    assertThat(statistics.getInputByteCount(), is(5L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getInputByteCount(), is(11L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getInputByteCount(), is(11L));
    assertThat(decorator.read(), is(-1));
    assertThat(statistics.getInputByteCount(), is(11L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputIteratorDisabled() throws IOException {
    getStatistics().setEnabled(false);

    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World").iterator(), CORR_ID);
    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.next();

    verifyNoStatistics(statistics);
  }

  @Test
  public void decorateInputListIterate() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID).iterator();

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.next();
    assertThat(statistics.getInputObjectCount(), is(1L));

    decorator.next();
    assertThat(statistics.getInputObjectCount(), is(2L));

    try {
      decorator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // nothing to do
    }
    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputListEnhancedFor() throws IOException {
    final Collection<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    for (String value : decorator) {
      // Nothing to do
    }

    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputListForEach() throws IOException {
    final Collection<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.forEach(value -> {
      // Nothing to do
    });

    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputListStream() throws IOException {
    final Collection<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.stream().forEach(value -> {
      // Nothing to do
    });

    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputListToArray() throws IOException {
    final Collection<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.toArray();

    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputListToArrayContent() throws IOException {
    final Collection<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World"), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.toArray(new String[0]);

    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputIteratorSingle() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World").iterator(), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.next();
    assertThat(statistics.getInputObjectCount(), is(1L));

    decorator.next();
    assertThat(statistics.getInputObjectCount(), is(2L));

    try {
      decorator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // nothing to do
    }
    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputIteratorForEachRemaining() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World").iterator(), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.forEachRemaining(s -> {
    });
    assertThat(statistics.getInputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputStreamDisabled() throws IOException {
    getStatistics().setEnabled(false);

    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);
    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read();
    assertThat(statistics.getInputByteCount(), is(0L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputStreamSingle() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputByteCount(), is(0L));

    decorator.read();
    assertThat(statistics.getOutputByteCount(), is(1L));

    decorator.read();
    assertThat(statistics.getOutputByteCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputStreamArray() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputByteCount(), is(0L));

    decorator.read(new byte[5]);
    assertThat(statistics.getOutputByteCount(), is(5L));

    decorator.read(new byte[10]);
    assertThat(statistics.getOutputByteCount(), is(11L));

    decorator.read(new byte[5]);
    assertThat(statistics.getOutputByteCount(), is(11L));
    assertThat(decorator.read(), is(-1));
    assertThat(statistics.getOutputByteCount(), is(11L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputStreamArrayBoundaries() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputByteCount(), is(0L));

    decorator.read(new byte[10], 2, 5);
    assertThat(statistics.getOutputByteCount(), is(5L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getOutputByteCount(), is(11L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getOutputByteCount(), is(11L));
    assertThat(decorator.read(), is(-1));
    assertThat(statistics.getOutputByteCount(), is(11L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputIteratorDisabled() throws IOException {
    getStatistics().setEnabled(false);

    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(asList("Hello", "World").iterator(), CORR_ID);
    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.next();
    assertThat(statistics.getOutputObjectCount(), is(0L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void decorateOutputIteratorSingle() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(asList("Hello", "World").iterator(), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.next();
    assertThat(statistics.getOutputObjectCount(), is(1L));

    decorator.next();
    assertThat(statistics.getOutputObjectCount(), is(2L));

    try {
      decorator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // nothing to do
    }
    assertThat(statistics.getOutputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void decorateOutputIteratorForEachRemaining() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(asList("Hello", "World").iterator(), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.forEachRemaining(s -> {
    });
    assertThat(statistics.getOutputObjectCount(), is(2L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void decorateOutputPagingProviderDisabled() throws IOException {
    getStatistics().setEnabled(false);

    final PagingProvider<Object, String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new TestPagingProvider(63, 20), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(0L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void decorateOutputPagingProvider() throws IOException {
    final PagingProvider<Object, String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new TestPagingProvider(63, 20), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(20L));
    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(40L));

    for (String string : decorator.getPage("conn")) {
      // nothing to do
    }
    assertThat(statistics.getOutputObjectCount(), is(60L));

    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(63L));
    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(63L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void decorateOutputPagingProviderOtherMethods() throws IOException, MuleException {
    final PagingProvider decorated = mock(PagingProvider.class);
    final int expectedTotal = 66;
    when(decorated.getTotalResults(any())).thenReturn(of(expectedTotal));

    final PagingProvider<Object, String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(decorated, CORR_ID);

    assertThat(decorator.getTotalResults("conn").get(), is(expectedTotal));

    decorator.close("conn");
    verify(decorated).close("conn");
  }

  @Test
  public void decorateOutputResultCollection() throws IOException {
    final List<Result> decorated = new ArrayList<>();

    for (int i = 0; i < 17; ++i) {
      decorated.add(Result.<InputStream, Object>builder()
          .output(new ByteArrayInputStream("Hello World".getBytes(UTF_8)))
          .build());
    }

    final Collection<Result> decorator =
        decoratorFactory.componentDecoratorFactory(component1).decorateOutputResultCollection(decorated, CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(17L));
    assertThat(statistics.getOutputByteCount(), is(0L));

    assertThat(decorator, hasSize(17));
    for (Result<InputStream, Object> result : decorator) {
      assertThat(IOUtils.toString(result.getOutput(), UTF_8), is("Hello World"));
    }


    assertThat(statistics.getOutputObjectCount(), is(17L));
    assertThat(statistics.getOutputByteCount(), is(17L * "Hello World".length()));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputResultCollectionNestedIterators() throws IOException {
    final List<Result> decorated = new ArrayList<>();

    for (int i = 0; i < 17; ++i) {
      decorated.add(Result.<Iterator<String>, Object>builder()
          .output(Arrays.asList("Hello", "World").iterator())
          .build());
    }

    final Collection<Result> decorator =
        decoratorFactory.componentDecoratorFactory(component1).decorateOutputResultCollection(decorated, CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(17L));
    assertThat(statistics.getOutputByteCount(), is(0L));

    assertThat(decorator, hasSize(17));
    for (Result<Iterator<String>, Object> result : decorator) {

      final Iterator<String> output = result.getOutput();
      while (output.hasNext()) {
        String nested = output.next();
        // Nothing to do
      }
    }

    assertThat(statistics.getOutputObjectCount(), is(17L * 2 + 17L));

    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputResultCollectionNestedNotStreamable() throws IOException {
    final List<Result> decorated = new ArrayList<>();

    for (int i = 0; i < 17; ++i) {
      decorated.add(Result.<String, Object>builder()
          .output("Hello World")
          .build());
    }

    final Collection<Result> decorator =
        decoratorFactory.componentDecoratorFactory(component1).decorateOutputResultCollection(decorated, CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(17L));
    assertThat(statistics.getOutputByteCount(), is(0L));

    assertThat(decorator, hasSize(17));
    for (Result<String, Object> result : decorator) {
      final String output = result.getOutput();
    }

    assertThat(statistics.getOutputObjectCount(), is(17L));

    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputResultIterator() throws IOException {
    final List<Result> decorated = new ArrayList<>();

    for (int i = 0; i < 17; ++i) {
      decorated.add(Result.<InputStream, Object>builder()
          .output(new ByteArrayInputStream("Hello World".getBytes(UTF_8)))
          .build());
    }

    final Iterator<Result> decorator =
        decoratorFactory.componentDecoratorFactory(component1).decorateOutputResultIterator(decorated.iterator(), CORR_ID);

    final PayloadStatistics statistics = getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getOutputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));

    while (decorator.hasNext()) {
      Result result = decorator.next();

      assertThat(IOUtils.toString((InputStream) result.getOutput(), UTF_8), is("Hello World"));
    }

    assertThat(statistics.getOutputObjectCount(), is(17L));
    assertThat(statistics.getOutputByteCount(), is(17L * "Hello World".length()));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputCursorStreamReturnsCursorStreamStatsEnabled() {
    verifyDecoratedCursorStreamIsCursorStream(true, decorateInputOperation(CORR_ID,
                                                                           decoratorFactory
                                                                               .componentDecoratorFactory(component1)));
  }

  @Test
  public void decorateInputCursorStreamReturnsCursorStreamStatsDisabled() {
    verifyDecoratedCursorStreamIsCursorStream(false,
                                              decorateInputOperation(CORR_ID,
                                                                     decoratorFactory.componentDecoratorFactory(component1)));
  }

  @Test
  public void decorateOuputCursorStreamReturnsCursorStreamStatsEnabled() {
    verifyDecoratedCursorStreamIsCursorStream(true, decorateInputOperation(CORR_ID,
                                                                           decoratorFactory
                                                                               .componentDecoratorFactory(component1)));
  }

  @Test
  public void decorateOutputCursorStreamReturnsCursorStreamStatsDisabled() {
    verifyDecoratedCursorStreamIsCursorStream(false,
                                              decorateOutputOperation(CORR_ID,
                                                                      decoratorFactory
                                                                          .componentDecoratorFactory(component1)));
  }

  @Test
  public void decorateOutputCursorStreamReturnsCursorStreamStatsEnabled() {
    verifyDecoratedCursorStreamIsCursorStream(true,
                                              decorateOutputOperation(CORR_ID,
                                                                      decoratorFactory
                                                                          .componentDecoratorFactory(component1)));
  }

  private void verifyDecoratedCursorStreamIsCursorStream(boolean statEnabled, UnaryOperator decorationOperator) {
    getStatistics().setEnabled(statEnabled);
    InputStream cursorStream = new ByteArrayCursorStream(mock(CursorStreamProvider.class), "Stream".getBytes(UTF_8));
    InputStream decorated = (InputStream) decorationOperator.apply(cursorStream);

    assertThat(decorated, instanceOf(CursorStream.class));
  }



  private void consumeInputStream() throws IOException {
    InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    InputStream decorator = decoratorFactory.componentDecoratorFactory(component1).decorateInput(decorated, CORR_ID);
    decorator.read();
  }
}
