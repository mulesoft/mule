/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.ComplexParameter;
import org.mule.test.some.extension.SomeAliasedParameterGroupOneRequiredConfig;
import org.mule.test.some.extension.SomeParameterGroupOneRequiredConfig;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(SOURCES)
public class SourceWithParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 1000;
  private static final List<CoreEvent> EVENTS = new LinkedList<>();

  @Override
  protected void doTearDown() throws Exception {
    EVENTS.clear();
  }

  @Override
  protected String getConfigFile() {
    return "values/some-source-parameter-group.xml";
  }

  @Test
  public void testWithSimpleParameter() throws Exception {
    startFlow("someParameter");
    assertEventsFired();
    assertThat(EVENTS.get(0).getMessage().getPayload().getValue(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameter() throws Exception {
    startFlow("complexParameter");
    assertEventsFired();
    assertThat(((ComplexParameter) EVENTS.get(0).getMessage().getPayload().getValue()).getAnotherParameter(),
               is("hello bird!"));
  }

  @Test
  public void testWithSimpleParameterDslTrue() throws Exception {
    startFlow("someParameterDslTrue");
    assertEventsFired();
    assertThat(EVENTS.get(0).getMessage().getPayload().getValue(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameterDslTrue() throws Exception {
    startFlow("complexParameterDslTrue");
    assertEventsFired();
    assertThat(((ComplexParameter) EVENTS.get(0).getMessage().getPayload().getValue()).getAnotherParameter(),
               is("hello bird!"));
  }

  @Test
  public void testShowInDslTrueWithRepeatedNameParameter() throws Exception {
    startFlow("dslTrueRepeatedNameParameter");
    assertEventsFired();
    SomeParameterGroupOneRequiredConfig config = getParameterGroupConfigValue();
    assertThat(config.getRepeatedNameParameter(), is("hello cat!"));
  }

  @Test
  public void testShowInDslTrueWithComplexParameterWithRepeatedNameParameter() throws Exception {
    startFlow("dslTrueComplexParameterWithRepeatedNameParameter");
    assertEventsFired();
    SomeParameterGroupOneRequiredConfig config = getParameterGroupConfigValue();
    Assert.assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInParameterGroup() throws Exception {
    startFlow("dslTrueRepeatedParameterNameInParameterGroup");
    assertEventsFired();
    ComplexParameter complexParameter = getComplexParameterValue();
    SomeParameterGroupOneRequiredConfig config = getParameterGroupConfigValue();
    Assert.assertThat(complexParameter.getRepeatedNameParameter(), is("hi lizard!"));
    Assert.assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInSource() throws Exception {
    startFlow("dslTrueRepeatedParameterNameInSource");
    assertEventsFired();
    SomeParameterGroupOneRequiredConfig config = getParameterGroupConfigValue();
    Assert.assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testShowInDslTrueWithComplexParameterWithParameterAlias() throws Exception {
    startFlow("dslTrueComplexParameterWithParameterAlias");
    assertEventsFired();
    SomeAliasedParameterGroupOneRequiredConfig config = getAliasedParameterGroupConfigValue();
    Assert.assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterAliasInSource() throws Exception {
    startFlow("dslTrueRepeatedParameterAliasInSource");
    assertEventsFired();
    SomeAliasedParameterGroupOneRequiredConfig config = getAliasedParameterGroupConfigValue();
    Assert.assertThat(config.getSomeParameter(), is("hello bird!"));
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

  private SomeParameterGroupOneRequiredConfig getParameterGroupConfigValue() {
    return (SomeParameterGroupOneRequiredConfig) ((Pair) EVENTS.get(0).getMessage().getPayload().getValue()).getFirst();
  }

  private ComplexParameter getComplexParameterValue() {
    return (ComplexParameter) ((Pair) EVENTS.get(0).getMessage().getPayload().getValue()).getSecond();
  }

  private SomeAliasedParameterGroupOneRequiredConfig getAliasedParameterGroupConfigValue() {
    return (SomeAliasedParameterGroupOneRequiredConfig) ((Pair) EVENTS.get(0).getMessage().getPayload().getValue()).getFirst();
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
