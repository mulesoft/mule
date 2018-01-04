/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.GroupCorrelation.of;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;

import org.junit.Test;

public class CorrelationInfoTestCase extends AbstractExtensionFunctionalTestCase {


  @Override
  protected String getConfigFile() {
    return "correlation-info-config.xml";
  }

  @Test
  public void defaultCorrelationInfo() throws Exception {
    final CoreEvent event = flowRunner("correlate").run();
    CorrelationInfo correlationInfo = (CorrelationInfo) event.getMessage().getPayload().getValue();
    assertThat(correlationInfo.getEventId(), is(event.getContext().getId()));
    assertThat(correlationInfo.isOutboundCorrelationEnabled(), is(true));
    assertThat(correlationInfo.getCorrelationId(), is(correlationInfo.getCorrelationId()));
    assertThat(correlationInfo.getGroupCorrelation(), is(nullValue()));
  }

  @Test
  public void customCorrelationId() throws Exception {
    final String correlationId = "correlateThis";
    final CoreEvent event = flowRunner("correlate").withSourceCorrelationId(correlationId).withCorrelation(of(43, 100)).run();
    CorrelationInfo correlationInfo = (CorrelationInfo) event.getMessage().getPayload().getValue();
    assertThat(correlationInfo.getEventId(), is(event.getContext().getId()));
    assertThat(correlationInfo.isOutboundCorrelationEnabled(), is(true));
    assertThat(correlationInfo.getCorrelationId(), is(correlationId));
    assertThat(correlationInfo.getGroupCorrelation(), is(notNullValue()));
    assertThat(correlationInfo.getGroupCorrelation().getGroupSize().getAsInt(), is(100));
    assertThat(correlationInfo.getGroupCorrelation().getSequence(), is(43));
  }
}
