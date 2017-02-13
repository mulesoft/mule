/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.simple.SetPayloadMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import org.junit.Test;

public class ConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-config.xml";
  }

  @Test
  public void globalObjectNotFound() {
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("nonExistent").build()).isPresent(),
               is(false));
  }

  @Test
  public void globalObjectFound() {
    Optional<Object> myFlow =
        muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build());
    assertThat(myFlow.isPresent(), is(true));
    assertThat(myFlow.get(), instanceOf(Flow.class));
  }

  @Test
  public void badContainerType() {
    Location location = builder().globalName("pepe").addProcessorsPart().addIndexPart(0).build();
    assertThat(muleContext.getConfigurationComponentLocator().find(location).isPresent(), is(false));
  }

  @Test
  public void sourceByPath() {
    Location sourceLocation = builder().globalName("myFlow").addPart("source").build();
    Optional<Object> source = muleContext.getConfigurationComponentLocator().find(sourceLocation);
    assertThat(source.isPresent(), is(true));
    assertThat(source.get(), instanceOf(MessageSource.class));
  }

  @Test
  public void messageProcessorByPath() {
    Location.Builder myFlowProcessorsLocationBuilder = builder().globalName("myFlow").addProcessorsPart();
    Optional<Object> processor =
        muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(LoggerMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(SetPayloadMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(2).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(AsyncDelegateMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(SetPayloadMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get(), instanceOf(LoggerMessageProcessor.class));
  }
}
