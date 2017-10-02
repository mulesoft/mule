/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Fork(1)
@Threads(1)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MICROSECONDS)
@State(Benchmark)
public class AbstractBenchmark {

  private final static Logger LOGGER = LoggerFactory.getLogger(AbstractBenchmark.class);

  public static final String CONNECTOR_NAME = "test";
  public static final String FLOW_NAME = "flow";
  public static final String PAYLOAD;
  public static final String KEY = "key";
  public static final String VALUE = "value";
  public static final ComponentLocation CONNECTOR_LOCATION = fromSingleComponent(CONNECTOR_NAME);

  static {
    try {
      PAYLOAD = getResourceAsString("test-data.json", AbstractBenchmark.class);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  protected MuleContext createMuleContextWithServices() throws MuleException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builderList = new ArrayList<>();
    builderList.add(new BasicRuntimeServicesConfigurationBuilder());
    builderList.add(new DefaultsConfigurationBuilder());
    return muleContextFactory.createMuleContext(builderList.toArray(new ConfigurationBuilder[] {}));
  }

  protected Flow createFlow(MuleContext muleContext) {
    return builder(FLOW_NAME, muleContext).build();
  }

  public CoreEvent createEvent(Flow flow) {
    return createEvent(flow, PAYLOAD);
  }

  public CoreEvent createEvent(Flow flow, Object payload) {
    try {
      return CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(of(payload)).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
