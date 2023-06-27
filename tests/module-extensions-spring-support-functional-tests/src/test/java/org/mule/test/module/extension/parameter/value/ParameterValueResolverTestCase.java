/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.ParameterGroupConfig;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
public class ParameterValueResolverTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 1000;
  private static final List<CoreEvent> EVENTS = new LinkedList<>();

  private static final ZonedDateTime ZONED_DATE_TIME_ON_CONFIG = ZonedDateTime.parse("2021-04-27T15:30Z");
  private static final ZonedDateTime ZONED_DATE_TIME_ON_SOURCE =
      ZonedDateTime.of(LocalDateTime.parse("2021-04-27T12:00"), ZoneId.of("America/Argentina/Buenos_Aires"));
  private static final ZonedDateTime ZONED_DATE_TIME_ON_OPERATION =
      ZonedDateTime.of(LocalDateTime.parse("2021-04-28T19:30:35"), ZoneOffset.of("+03:00"));

  @Override
  protected void doTearDown() throws Exception {
    EVENTS.clear();
  }

  @Override
  protected String getConfigFile() {
    return "parameter/parameter-value-resolver.xml";
  }

  @Test
  public void configurationWithZonedDateTimeParameter() throws Exception {
    ParameterGroupConfig configuration = getPayload("configurationWithZonedDateTimeParameter");
    boolean isEqual = ZONED_DATE_TIME_ON_CONFIG.isEqual(configuration.getZonedDateTime());
    assertThat(isEqual, is(true));
  }

  @Test
  public void sourceWithZonedDateTimeParameter() throws Exception {
    startFlow("sourceWithZonedDateTimeParameter");
    assertEventsFired();
    boolean isEqual = ZONED_DATE_TIME_ON_SOURCE.isEqual((ZonedDateTime) EVENTS.get(0).getMessage().getPayload().getValue());
    assertThat(isEqual, is(true));
  }

  @Test
  public void operationWithZonedDateTimeParameter() throws Exception {
    ZonedDateTime zonedDateTime = getPayload("operationWithZonedDateTimeParameter");
    boolean isEqual = ZONED_DATE_TIME_ON_OPERATION.isEqual(zonedDateTime);
    assertThat(isEqual, is(true));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void assertEventsFired() {
    check(TIMEOUT, DELAY, () -> {
      synchronized (EVENTS) {
        return EVENTS.size() >= 1;
      }
    });
  }

  private <T> T getPayload(String flowName) throws Exception {
    return (T) flowRunner(flowName).run().getMessage().getPayload().getValue();
  }

  public static class CaptureProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
      }
      return event;
    }
  }
}
