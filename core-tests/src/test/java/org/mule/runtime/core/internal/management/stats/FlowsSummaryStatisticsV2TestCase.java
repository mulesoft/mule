/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.APIKIT;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.GENERIC;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.SOAPKIT;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.FlowSummaryStory.ACTIVE_FLOWS_SUMMARY;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.PRICING_METRICS;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.management.stats.FlowsSummaryStatistics;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractPipeline;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@SmallTest
@Feature(PRICING_METRICS)
@Story(ACTIVE_FLOWS_SUMMARY)
// TODO W-18668900: swap with FlowsSummaryStatisticsTestCase and remove once the pilot is concluded
public class FlowsSummaryStatisticsV2TestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    muleContext = spy(muleContext);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    FlowClassifier mockFlowClassifier = mock(FlowClassifier.class);
    when(mockFlowClassifier.getFlowType(anyString())).thenReturn(GENERIC);
    when(mockFlowClassifier.getFlowType(endsWith(":api-config"))).thenReturn(APIKIT);
    when(mockFlowClassifier.getFlowType(endsWith(":\\soapkit-config"))).thenReturn(SOAPKIT);
    return singletonMap("_FlowClassifier", mockFlowClassifier);
  }

  @Test
  public void triggerFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void apikitFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("get:\\reservation:api-config", muleContext,
                                         null,
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 1, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void apikitWithSourceFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("get:\\reservation:api-config", muleContext,
                                         mock(MessageSource.class),
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  @Description("Even though the flow name matches the ApiKit pattern, it does not match with any ApiKit config, so it is considered as non-ApiKit")
  public void almostApikitFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("get:\\reservation:api-config-impl", muleContext,
                                         null,
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 1);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void soapkitFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("OrderTshirt:\\soapkit-config", muleContext,
                                         null,
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 1, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void soapkitWithSourceFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("OrderTshirt:\\soapkit-config", muleContext,
                                         mock(MessageSource.class),
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  @Description("Even though the flow name matches the Soapkit pattern, it does not match with any Soapkit config, so it is considered as non-Soapkit")
  public void almostSoapkitFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("OrderTshirt:\\soapkit-config-impl", muleContext,
                                         null,
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 1);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void privateFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         null,
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 1);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void twoTriggerFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow1 = new TestPipeline("", muleContext,
                                          mock(MessageSource.class),
                                          INITIAL_STATE_STARTED,
                                          flowsSummaryStatistics);
    TestPipeline flow2 = new TestPipeline("", muleContext,
                                          mock(MessageSource.class),
                                          INITIAL_STATE_STARTED,
                                          flowsSummaryStatistics);

    flow1.initialise();
    flow2.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);

    flow1.start();
    flow2.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 2, 0, 0);

    flow1.stop();
    flow2.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);

    flow1.dispose();
    flow2.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void triggerFlowRestarted() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         INITIAL_STATE_STARTED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void triggerFlowInitialStateStopped() throws MuleException {
    doReturn(true).when(muleContext).isStarting();

    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         INITIAL_STATE_STOPPED,
                                         flowsSummaryStatistics);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    doReturn(false).when(muleContext).isStarting();
    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  private void assertFlowsSummaryStatistics(FlowsSummaryStatistics flowsSummaryStatistics,
                                            int expectedDeclaredTriggerFlows,
                                            int expectedDeclaredApikitFlows,
                                            int expectedDeclaredPrivateFlows,
                                            int expectedActiveTriggerFlows,
                                            int expectedActiveApikitFlows,
                                            int expectedActivePrivateFlows) {
    assertThat("declaredTriggerFlows",
               flowsSummaryStatistics.getDeclaredTriggerFlows(), is(expectedDeclaredTriggerFlows));
    assertThat("declaredApikitFlows",
               flowsSummaryStatistics.getDeclaredApikitFlows(), is(expectedDeclaredApikitFlows));
    assertThat("declaredPrivateFlows",
               flowsSummaryStatistics.getDeclaredPrivateFlows(), is(expectedDeclaredPrivateFlows));
    assertThat("activeTriggerFlows",
               flowsSummaryStatistics.getActiveTriggerFlows(), is(expectedActiveTriggerFlows));
    assertThat("activeApikitFlows",
               flowsSummaryStatistics.getActiveApikitFlows(), is(expectedActiveApikitFlows));
    assertThat("activePrivateFlows",
               flowsSummaryStatistics.getActivePrivateFlows(), is(expectedActivePrivateFlows));
  }

  private static class TestPipeline extends AbstractPipeline {

    public TestPipeline(String name, MuleContext muleContext, MessageSource source, String initialState,
                        DefaultFlowsSummaryStatistics flowsSummaryStatistics) {
      super(name, muleContext, source, emptyList(), empty(), empty(), initialState, null,
            new DefaultFlowsSummaryStatistics(true), flowsSummaryStatistics, null, null);
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class, RETURNS_DEEP_STUBS);
    }
  }

}
