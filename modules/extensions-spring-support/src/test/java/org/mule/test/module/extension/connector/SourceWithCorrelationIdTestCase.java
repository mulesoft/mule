/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

  private static String correlationId;

  @Override
  protected String getConfigFile() {
    return "source-with-correlation-id-config.xml";
  }

  @Before
  public void setUp() {
    correlationId = null;
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
