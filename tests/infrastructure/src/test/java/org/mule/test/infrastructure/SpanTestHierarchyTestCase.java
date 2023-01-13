/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.OTEL_EXCEPTION_ESCAPED_KEY;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.OTEL_EXCEPTION_EVENT_NAME;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.OTEL_EXCEPTION_MESSAGE_KEY;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.OTEL_EXCEPTION_STACK_TRACE_KEY;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.OTEL_EXCEPTION_TYPE_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SpanTestHierarchyTestCase extends AbstractMuleTestCase {

  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlation.id";
  public static final String ARTIFACT_ID_KEY = "artifact.id";
  public static final String THREAD_START_ID_KEY = "thread.start.id";
  public static final String THREAD_END_NAME_KEY = "thread.end.name";
  public static final String ARTIFACT_TYPE_ID = "artifact.type";

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
  private static final String LOGGER_SPAN_ID_2 = "1234logger1234";
  private static final String LOGGER_SPAN_ID_3 = "12345logger12345";
  private static final String SET_PAYLOAD_SPAN_ID_1 = "123setpayload1123";
  private static final String SET_PAYLOAD_SPAN_ID_2 = "123setpayload2123";
  private static final String SET_VARIABLE_SPAN_ID = "123setvariable123";
  public static final String TEST_ARTIFACT_ID = "SpanTestHierarchyTestCase#mockCapturedExportedSpan";

  public static final String ERROR_TYPE_1 = "CUSTOM:ERROR";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void testWhenSimpleStructureSpanTreeMatchesExpectedSpansAssertionShouldNotFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather1 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather2 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute1 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute2 = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather1 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather2 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute1 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute2 = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload1 = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload2 = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload1 = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload2 = mockCapturedExportedSpan();

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
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGather = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute1 = mockCapturedExportedSpan();
    CapturedExportedSpan scatterGatherRoute2 = mockCapturedExportedSpan();
    CapturedExportedSpan setPayload = mockCapturedExportedSpan();
    CapturedExportedSpan setVariable = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

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

  @Test
  public void testWhenAttributesInTreeMatchActualAttributesAssertionShouldNotFailWithThreeLoggersInSameAndDifferentLocations() {
    String flowLocation = "flow-test-location";
    String asyncLocation = "async-test-location";
    String loggerLocation = "logger-test-location";
    String loggerSecondLocation = "logger-second-test-location";

    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();
    CapturedExportedSpan secondLogger = mockCapturedExportedSpan();
    CapturedExportedSpan thirdLogger = mockCapturedExportedSpan();

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    muleFlow.getAttributes().put(LOCATION_KEY, flowLocation);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    async.getAttributes().put(LOCATION_KEY, asyncLocation);

    when(logger.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);
    logger.getAttributes().put(LOCATION_KEY, loggerLocation);

    when(thirdLogger.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(thirdLogger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(thirdLogger.getSpanId()).thenReturn(LOGGER_SPAN_ID_3);
    thirdLogger.getAttributes().put(LOCATION_KEY, loggerLocation);

    when(secondLogger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(secondLogger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(secondLogger.getSpanId()).thenReturn(LOGGER_SPAN_ID_2);
    secondLogger.getAttributes().put(LOCATION_KEY, loggerSecondLocation);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);
    capturedExportedSpans.add(secondLogger);
    capturedExportedSpans.add(thirdLogger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(flowLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .child(EXPECTED_ASYNC_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(asyncLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerSecondLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenAttributesInTreeDontMatchActualAttributesAssertionShouldFailWithThreeLoggersInSameAndDifferentLocations() {
    String flowLocation = "flow-test-location";
    String asyncLocation = "async-test-location";
    String loggerLocation = "logger-test-location";
    String loggerSecondLocation = "logger-second-test-location";

    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();
    CapturedExportedSpan secondLogger = mockCapturedExportedSpan();
    CapturedExportedSpan thirdLogger = mockCapturedExportedSpan();

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    muleFlow.getAttributes().put(LOCATION_KEY, flowLocation);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    async.getAttributes().put(LOCATION_KEY, asyncLocation);

    when(logger.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);
    logger.getAttributes().put(LOCATION_KEY, loggerLocation);

    // This logger has the Async scope as parent instead of the expected Mule Flow parent
    when(thirdLogger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(thirdLogger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(thirdLogger.getSpanId()).thenReturn(LOGGER_SPAN_ID_3);
    thirdLogger.getAttributes().put(LOCATION_KEY, loggerLocation);

    when(secondLogger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(secondLogger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(secondLogger.getSpanId()).thenReturn(LOGGER_SPAN_ID_2);
    secondLogger.getAttributes().put(LOCATION_KEY, loggerSecondLocation);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);
    capturedExportedSpans.add(secondLogger);
    capturedExportedSpans.add(thirdLogger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(flowLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .child(EXPECTED_ASYNC_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(asyncLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(loggerSecondLocation, TEST_ARTIFACT_ID))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected span: mule:logger was not found");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenExceptionsInTreeMatchActualExceptionsAssertionShouldNotFail() {
    Map<String, Object> exceptionAttributes = new HashMap<>();
    exceptionAttributes.put(OTEL_EXCEPTION_TYPE_KEY, ERROR_TYPE_1);
    exceptionAttributes.put(OTEL_EXCEPTION_MESSAGE_KEY, "An error occurred.");
    exceptionAttributes.put(OTEL_EXCEPTION_ESCAPED_KEY, "true");
    exceptionAttributes.put(OTEL_EXCEPTION_STACK_TRACE_KEY, "Test stack trace");

    CapturedEventData capturedEventData = mock(CapturedEventData.class);
    when(capturedEventData.getName()).thenReturn(OTEL_EXCEPTION_EVENT_NAME);
    when(capturedEventData.getAttributes()).thenReturn(exceptionAttributes);

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(async.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(async.hasErrorStatus()).thenReturn(true);

    when(logger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenExceptionsInTreeDoesNotMatchActualExceptionsAssertionShouldFail() {
    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

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
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected exceptions for Span: [Mock for CapturedExportedSpan, hashCode: ");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenExceptionAttributeInTreeDoesNotMatchActualExceptionAttributeAssertionShouldFail() {
    Map<String, Object> exceptionAttributes = new HashMap<>();
    exceptionAttributes.put(OTEL_EXCEPTION_TYPE_KEY, "Wrong Error type");
    exceptionAttributes.put(OTEL_EXCEPTION_MESSAGE_KEY, "An error occurred.");
    exceptionAttributes.put(OTEL_EXCEPTION_ESCAPED_KEY, "true");
    exceptionAttributes.put(OTEL_EXCEPTION_STACK_TRACE_KEY, "Test stack trace");

    CapturedEventData capturedEventData = mock(CapturedEventData.class);
    when(capturedEventData.getName()).thenReturn(OTEL_EXCEPTION_EVENT_NAME);
    when(capturedEventData.getAttributes()).thenReturn(exceptionAttributes);

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(async.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(async.hasErrorStatus()).thenReturn(true);

    when(logger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expected: \"CUSTOM:ERROR\"\n" +
        "     but: was \"Wrong Error type");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenThereIsANotExpectedExceptionAssertionShouldFail() {
    Map<String, Object> exceptionAttributes = new HashMap<>();
    exceptionAttributes.put(OTEL_EXCEPTION_TYPE_KEY, ERROR_TYPE_1);
    exceptionAttributes.put(OTEL_EXCEPTION_MESSAGE_KEY, "An error occurred.");
    exceptionAttributes.put(OTEL_EXCEPTION_ESCAPED_KEY, "true");
    exceptionAttributes.put(OTEL_EXCEPTION_STACK_TRACE_KEY, "Test stack trace");

    CapturedEventData capturedEventData = mock(CapturedEventData.class);
    when(capturedEventData.getName()).thenReturn(OTEL_EXCEPTION_EVENT_NAME);
    when(capturedEventData.getAttributes()).thenReturn(exceptionAttributes);

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

    // Exception added to muleFlow which is not expected by the span tree
    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(muleFlow.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(muleFlow.hasErrorStatus()).thenReturn(true);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(async.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(async.hasErrorStatus()).thenReturn(true);

    when(logger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Unexpected Span exceptions found for Span: [Mock for CapturedExportedSpan, hashCode: ");

    spanTestHierarchy.assertSpanTree();
  }

  @Test
  public void testWhenNoExceptionDataIsExpectedAssertionShouldNotFail() {
    Map<String, Object> exceptionAttributes = new HashMap<>();
    exceptionAttributes.put(OTEL_EXCEPTION_TYPE_KEY, ERROR_TYPE_1);
    exceptionAttributes.put(OTEL_EXCEPTION_MESSAGE_KEY, "An error occurred.");
    exceptionAttributes.put(OTEL_EXCEPTION_ESCAPED_KEY, "true");
    exceptionAttributes.put(OTEL_EXCEPTION_STACK_TRACE_KEY, "Test stack trace");

    CapturedEventData capturedEventData = mock(CapturedEventData.class);
    when(capturedEventData.getName()).thenReturn(OTEL_EXCEPTION_EVENT_NAME);
    when(capturedEventData.getAttributes()).thenReturn(exceptionAttributes);

    List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();
    CapturedExportedSpan muleFlow = mockCapturedExportedSpan();
    CapturedExportedSpan async = mockCapturedExportedSpan();
    CapturedExportedSpan logger = mockCapturedExportedSpan();

    when(muleFlow.getParentSpanId()).thenReturn(NO_PARENT_SPAN);
    when(muleFlow.getName()).thenReturn(EXPECTED_FLOW_SPAN_NAME);
    when(muleFlow.getSpanId()).thenReturn(MULE_FLOW_SPAN_ID);

    when(async.getParentSpanId()).thenReturn(MULE_FLOW_SPAN_ID);
    when(async.getName()).thenReturn(EXPECTED_ASYNC_SPAN_NAME);
    when(async.getSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(async.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(async.hasErrorStatus()).thenReturn(true);

    when(logger.getParentSpanId()).thenReturn(ASYNC_SPAN_ID);
    when(logger.getName()).thenReturn(EXPECTED_LOGGER_SPAN_NAME);
    when(logger.getSpanId()).thenReturn(LOGGER_SPAN_ID);
    when(logger.getEvents()).thenReturn(Collections.singletonList(capturedEventData));
    when(logger.hasErrorStatus()).thenReturn(true);

    capturedExportedSpans.add(muleFlow);
    capturedExportedSpans.add(async);
    capturedExportedSpans.add(logger);

    SpanTestHierarchy spanTestHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    spanTestHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_ASYNC_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    spanTestHierarchy.assertSpanTree();
  }

  private CapturedExportedSpan mockCapturedExportedSpan() {
    CapturedExportedSpan mockedSpan = mock(CapturedExportedSpan.class);

    // mocking start and end epoch nanos.
    when(mockedSpan.getStartEpochSpanNanos()).thenReturn(0L);
    when(mockedSpan.getEndSpanEpochNanos()).thenReturn(1L);

    Map<String, String> basicAttributes = new HashMap<>();
    basicAttributes.put(CORRELATION_ID_KEY, "test-correlation-id");
    basicAttributes.put(THREAD_START_ID_KEY, "12");
    basicAttributes.put(THREAD_END_NAME_KEY, "endThread");
    basicAttributes.put(ARTIFACT_ID_KEY, TEST_ARTIFACT_ID);
    basicAttributes.put(ARTIFACT_TYPE_ID, APP.getAsString());
    when(mockedSpan.getAttributes()).thenReturn(basicAttributes);
    when(mockedSpan.getServiceName()).thenReturn(TEST_ARTIFACT_ID);
    when(mockedSpan.getTraceId()).thenReturn("1-test-trace-id-1");
    return mockedSpan;
  }
}
