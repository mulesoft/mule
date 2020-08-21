/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

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
    muleContext.getStatistics().setEnabled(false);

    final InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1).decorateInput(decorated, CORR_ID);

    assertThat(decorated, sameInstance(decorator));

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read();

    verifyNoStatistics(statistics);
  }

  @Test
  public void decorateInputStreamNotMixesComponents() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);
    decoratorFactory.componentDecoratorFactory(component2);

    final PayloadStatistics statistics2 =
        muleContext.getStatistics().getPayloadStatistics(component2.getLocation().getLocation());

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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read(new byte[5]);
    assertThat(statistics.getInputByteCount(), is(5L));

    decorator.read(new byte[10]);
    assertThat(statistics.getInputByteCount(), is(11L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputStreamArrayBoundaries() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getInputByteCount(), is(0L));

    decorator.read(new byte[10], 2, 5);
    assertThat(statistics.getInputByteCount(), is(5L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getInputByteCount(), is(11L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateInputIteratorDisabled() throws IOException {
    muleContext.getStatistics().setEnabled(false);

    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World").iterator(), CORR_ID);
    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

    assertThat(statistics.getInputObjectCount(), is(0L));

    decorator.next();

    verifyNoStatistics(statistics);
  }

  @Test
  public void decorateInputIteratorSingle() throws IOException {
    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateInput(asList("Hello", "World").iterator(), CORR_ID);

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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
    muleContext.getStatistics().setEnabled(false);

    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);
    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputByteCount(), is(0L));

    decorator.read(new byte[5]);
    assertThat(statistics.getOutputByteCount(), is(5L));

    decorator.read(new byte[10]);
    assertThat(statistics.getOutputByteCount(), is(11L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputStreamArrayBoundaries() throws IOException {
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new ByteArrayInputStream("Hello World".getBytes(UTF_8)), CORR_ID);

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputByteCount(), is(0L));

    decorator.read(new byte[10], 2, 5);
    assertThat(statistics.getOutputByteCount(), is(5L));

    decorator.read(new byte[10], 0, 10);
    assertThat(statistics.getOutputByteCount(), is(11L));

    assertThat(statistics.getInputByteCount(), is(0L));
    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  @Test
  public void decorateOutputIteratorDisabled() throws IOException {
    muleContext.getStatistics().setEnabled(false);

    final Iterator<String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(asList("Hello", "World").iterator(), CORR_ID);
    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
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
    muleContext.getStatistics().setEnabled(false);

    final PagingProvider<Object, String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(new TestPagingProvider(63, 20), CORR_ID);

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());

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

    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component1.getLocation().getLocation());
    assertThat(statistics.getOutputObjectCount(), is(0L));

    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(20L));
    decorator.getPage("conn");
    assertThat(statistics.getOutputObjectCount(), is(40L));

    for (String string : decorator.getPage("conn")) {

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
    when(decorated.getTotalResults(Mockito.any())).thenReturn(Optional.of(expectedTotal));

    final PagingProvider<Object, String> decorator = decoratorFactory.componentDecoratorFactory(component1)
        .decorateOutput(decorated, CORR_ID);

    assertThat(decorator.getTotalResults("conn").get(), is(expectedTotal));

    decorator.close("conn");
    verify(decorated).close("conn");
  }

}
