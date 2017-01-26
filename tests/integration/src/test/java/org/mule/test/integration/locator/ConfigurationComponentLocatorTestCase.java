/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.ObjectNotFoundException;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.simple.SetPayloadMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-config.xml";
  }

  @Test(expected = ObjectNotFoundException.class)
  public void globalObjectNotFound() {
    muleContext.getConfigurationComponentLocator().findByName("nonExistent");
  }

  public void globalObjectFound() {
    Object flow = muleContext.getConfigurationComponentLocator().findByName("myFlow");
    assertThat(flow, instanceOf(Flow.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void badContainerType() {
    muleContext.getConfigurationComponentLocator().findByPath("pepe/processors/0");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badContainerPart() {
    muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow/inputPhase");
  }

  @Test(expected = ObjectNotFoundException.class)
  public void badContainerName() {
    muleContext.getConfigurationComponentLocator().findByPath("flow/notExistentFlow");
  }

  public void flowByPath() {
    Object flow = muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow");
    assertThat(flow, instanceOf(Flow.class));
  }

  public void sourceByPath() {
    Object source = muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow/source");
    assertThat(source, instanceOf(MessageSource.class));
  }

  public void messageProcessorByPath() {
    Object processor = muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow/processors/0");
    assertThat(processor, instanceOf(LoggerMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow/processors/1");
    assertThat(processor, instanceOf(SetPayloadMessageProcessor.class));
    processor = muleContext.getConfigurationComponentLocator().findByPath("flow/myFlow/processors/2");
    assertThat(processor, instanceOf(AsyncDelegateMessageProcessor.class));
  }
}
