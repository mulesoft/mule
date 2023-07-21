/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.connector;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public class SourceWithCorrelationIdTestCase extends AbstractExtensionFunctionalTestCase {

  private static volatile String correlationId;

  @Override
  protected String getConfigFile() {
    return "source-with-correlation-id-config.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    correlationId = null;
  }

  @Test
  public void captureCorrelationId() throws Exception {
    PollingProber.check(5000, 100, () -> "Primate".equals(correlationId));
  }

  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      correlationId = event.getCorrelationId();
      return event;
    }
  }

}
