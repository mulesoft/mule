/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SpanTestHierarchyTestCase extends AbstractMuleTestCase {

  private static final String NO_PARENT_SPAN = "0000000000000000";

  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_ASYNC_SPAN_NAME = "mule:async";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SCATTER_GATHER_SPAN_NAME = "mule:scatter-gather";
  public static final String EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME = "mule:scatter-gather:route";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";

  private static final String MULE_FLOW_SPAN_ID = "123flow123";
  private static final String ASYNC_SPAN_ID = "123async123";
  private static final String SCATTER_GATHER_SPAN_ID_1 = "123scattergather1123";
  private static final String SCATTER_GATHER_SPAN_ID_2 = "123scattergather2123";
  private static final String SCATTER_GATHER_ROUTE_SPAN_ID_1 = "123scattergatherroute1123";
  private static final String SCATTER_GATHER_ROUTE_SPAN_ID_2 = "123scattergatherroute2123";
  private static final String LOGGER_SPAN_ID = "123logger123";
  private static final String SET_PAYLOAD_SPAN_ID_1 = "123setpayload1123";
  private static final String SET_PAYLOAD_SPAN_ID_2 = "123setpayload2123";
  private static final String SET_VARIABLE_SPAN_ID = "123setvariable123";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void testWhenSimpleStructureSpanTreeMatchesExpectedSpansAssertionShouldNotFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan async = mock(CapturedExportedSpan.class);
    CapturedExportedSpan logger = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);

    when(logger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  @Test()
  public void testWhenSimpleStructureSpanTreeDoesNotMatchExpectedSpansAssertionShouldFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan async = mock(CapturedExportedSpan.class);
    CapturedExportedSpan logger = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);

    // Actual logger has different parent id from expected logger
    when(logger.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected span: mule:logger was not found");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenSpanTreeWithTwoChildrenWithSameNameMatchesExpectedSpansAssertionShouldNotFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan logger = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(scatterGather1.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather1.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather1.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);

    when(scatterGather2.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather2.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather2.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_2);

    when(scatterGatherRoute1.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute1.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute1.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);

    when(scatterGatherRoute2.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_2);
    when(scatterGatherRoute2.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute2.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);

    when(logger.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    when(setPayload.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);
    when(setPayload.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_1);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(scatterGather1);
    capturedExportedSpans.add(scatterGather2);
    capturedExportedSpans.add(scatterGatherRoute1);
    capturedExportedSpans.add(scatterGatherRoute2);
    capturedExportedSpans.add(logger);
    capturedExportedSpans.add(setPayload);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren()
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenSpanTreeWithTwoChildrenWithSameNameDoesNotMatchExpectedSpansAssertionShouldFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan logger = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(scatterGather1.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather1.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather1.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);

    when(scatterGather2.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather2.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather2.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_2);

    when(scatterGatherRoute1.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute1.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute1.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);

    when(scatterGatherRoute2.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_2);
    when(scatterGatherRoute2.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute2.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);

    when(logger.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    when(setPayload.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);
    when(setPayload.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_1);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(scatterGather1);
    capturedExportedSpans.add(scatterGather2);
    capturedExportedSpans.add(scatterGatherRoute1);
    capturedExportedSpans.add(scatterGatherRoute2);
    capturedExportedSpans.add(logger);
    capturedExportedSpans.add(setPayload);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME) // Expecting logger but actually a setPayload was captured
        .endChildren()
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected span: mule:logger was not found");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenSpanTreeWithTwoSetPayloadsOnDifferentBranchesMatchesExpectedSpansAssertionShouldNotFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload2 = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(scatterGather.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);

    when(scatterGatherRoute.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);

    when(setPayload1.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(setPayload1.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload1.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_1);

    when(setPayload2.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(setPayload2.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload2.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_2);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(scatterGather);
    capturedExportedSpans.add(scatterGatherRoute);
    capturedExportedSpans.add(setPayload1);
    capturedExportedSpans.add(setPayload2);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren()
        .endChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenSpanTreeWithTwoSetPayloadsOnDifferentBranchesDoesNotMatchExpectedSpansAssertionShouldFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload2 = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(scatterGather.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);

    when(scatterGatherRoute.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);

    when(setPayload1.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(setPayload1.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload1.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_1);

    // Second setPayload has different parent than expected
    when(setPayload2.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(setPayload2.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload2.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_2);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(scatterGather);
    capturedExportedSpans.add(scatterGatherRoute);
    capturedExportedSpans.add(setPayload1);
    capturedExportedSpans.add(setPayload2);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren()
        .endChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected span: mule:set-payload was not found");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testDeclaringOrderWhenCreatingTreeShouldNotChangeTheTree() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGather = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan scatterGatherRoute2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setPayload = mock(CapturedExportedSpan.class);
    CapturedExportedSpan setVariable = mock(CapturedExportedSpan.class);
    CapturedExportedSpan logger = mock(CapturedExportedSpan.class);

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(scatterGather.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(scatterGather.getName()).thenReturn(EXPECTED_SCATTER_GATHER_SPAN_NAME);
    when(scatterGather.getSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);

    when(scatterGatherRoute1.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute1.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute1.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);

    when(setPayload.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_1);
    when(setPayload.getName()).thenReturn(EXPECTED_SET_PAYLOAD_SPAN_NAME);
    when(setPayload.getSpanId()).thenReturn(SET_PAYLOAD_SPAN_ID_1);

    when(scatterGatherRoute2.getParentSpanId()).thenReturn(SCATTER_GATHER_SPAN_ID_1);
    when(scatterGatherRoute2.getName()).thenReturn(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME);
    when(scatterGatherRoute2.getSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);

    when(setVariable.getParentSpanId()).thenReturn(SCATTER_GATHER_ROUTE_SPAN_ID_2);
    when(setVariable.getName()).thenReturn(EXPECTED_SET_VARIABLE_SPAN_NAME);
    when(setVariable.getSpanId()).thenReturn(SET_VARIABLE_SPAN_ID);

    when(logger.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(scatterGather);
    capturedExportedSpans.add(scatterGatherRoute1);
    capturedExportedSpans.add(scatterGatherRoute2);
    capturedExportedSpans.add(setPayload);
    capturedExportedSpans.add(setVariable);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
        .endChildren()
        .endChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren();

    SpanTestHierarchy spanTestHierarchyAlternativeOrder = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchyAlternativeOrder.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
        .endChildren()
        .child(EXPECTED_SCATTER_GATHER_ROUTE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .endChildren()
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
    spanTestHierarchyAlternativeOrder.assertSpanTree();
  }
}
