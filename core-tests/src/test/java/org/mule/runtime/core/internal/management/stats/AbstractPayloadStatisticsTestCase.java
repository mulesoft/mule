/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;

public class AbstractPayloadStatisticsTestCase extends AbstractMuleContextTestCase {

  protected static final String CORR_ID = "corrId";

  protected CursorDecoratorFactory decoratorFactory;
  protected Component component1;
  protected Component component2;
  protected Component componentNoLocation;

  @Before
  public void before() throws RegistrationException, InitialisationException {
    decoratorFactory = new PayloadStatisticsCursorDecoratorFactory();
    initialiseIfNeeded(decoratorFactory, muleContext);

    muleContext.getStatistics().setEnabled(true);

    component1 = mock(Component.class);
    when(component1.getIdentifier()).thenReturn(buildFromStringRepresentation("ns:comp1"));
    when(component1.getLocation()).thenReturn(fromSingleComponent("component1"));

    component2 = mock(Component.class);
    when(component2.getIdentifier()).thenReturn(buildFromStringRepresentation("ns:comp2"));
    when(component2.getLocation()).thenReturn(fromSingleComponent("component2"));

    componentNoLocation = mock(Component.class);
    when(componentNoLocation.getIdentifier()).thenReturn(buildFromStringRepresentation("ns:comp3"));
    when(componentNoLocation.getLocation()).thenReturn(null);
  }

  protected void verifyNoStatistics(final PayloadStatistics statistics) throws IOException {
    assertThat(statistics.getInputByteCount(), is(0L));

    assertThat(statistics.getInputObjectCount(), is(0L));
    assertThat(statistics.getOutputByteCount(), is(0L));
    assertThat(statistics.getOutputObjectCount(), is(0L));
  }

  public class TestPagingProvider implements PagingProvider<Object, String> {

    private final int totalSize;
    private final int pageSize;

    private long counter = 0;

    public TestPagingProvider(int totalSize, int pageSize) {
      this.totalSize = totalSize;
      this.pageSize = pageSize;
    }

    @Override
    public List<String> getPage(Object con) {
      if (counter < totalSize) {
        List<String> page = new ArrayList<>(pageSize);
        for (int i = 0; i < pageSize && counter < totalSize; i++) {
          counter++;
          String value = randomAlphabetic(5000);
          page.add(value);
        }

        return page;
      }

      return emptyList();
    }

    @Override
    public void close(Object con) throws MuleException {}

    @Override
    public Optional<Integer> getTotalResults(Object con) {
      return of(totalSize);
    }
  }

  protected AllStatistics getStatistics() {
    return muleContext.getStatistics();
  }
}
