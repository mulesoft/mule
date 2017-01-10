/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.config.builders.BasicRuntimeServicesConfigurationBuilder;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.DefaultMuleContextFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Static util methods for use in benchmark setup/teardown. Benchmark methods themselves should ideally be self-contained for
 * clarity.
 */
public class BenchmarkUtils {

  public static final String CONNECTOR_NAME = "test";
  public static final String FLOW_NAME = "flow";
  public static final String PAYLOAD;
  public static final String KEY = "key";
  public static final String VALUE = "value";

  static {
    try {
      PAYLOAD = getResourceAsString("test-data.json", BenchmarkUtils.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static MuleContext createMuleContext() throws MuleException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    return muleContextFactory.createMuleContext();
  }

  public static MuleContext createMuleContextWithServices() throws MuleException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builderList = new ArrayList<>();
    builderList.add(new DefaultsConfigurationBuilder());
    builderList.add(new BasicRuntimeServicesConfigurationBuilder());
    return muleContextFactory.createMuleContext(builderList.toArray(new ConfigurationBuilder[] {}));
  }

  public static Flow createFlow(MuleContext muleContext) {
    return new Flow(FLOW_NAME, muleContext);
  }

  public static Event createEvent(Flow flow) {
    try {
      return Event.builder(DefaultEventContext.create(flow, CONNECTOR_NAME))
          .message(InternalMessage.builder().payload(PAYLOAD).build()).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
