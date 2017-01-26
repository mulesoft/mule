/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.typed.value;

import org.junit.After;
import org.junit.Test;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypedValueParameterOperationExecutionTestCase extends AbstractTypedValueTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {TypedValueExtension.class};
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"typed-value-config.xml"};
  }

  @After
  public void cleanUp() {
    TypedValueSource.onSuccessValue = null;
  }

  @Test
  public void typedValueForString() throws Exception {
    runAndAssertTypedValue("typedValueForString", "This is a string", MediaType.APPLICATION_JSON, UTF8);
  }

  @Test
  public void typedValueForStringFromByteArray() throws Exception {
    runAndAssertTypedValue("typedValueForStringFromByteArray", "This is a string", MediaType.APPLICATION_JSON, UTF8);
  }

  @Test
  public void typedValueForStringWithDefaultValue() throws Exception {
    runAndAssertTypedValue("typedValueForStringWithDefaultValue", THIS_IS_A_DEFAULT_STRING, MediaType.ANY, null);
  }

  @Test
  public void typedValueForStringList() throws Exception {
    List<Object> strings = new ArrayList<>();
    strings.add("string");
    strings.add("string");
    runAndAssertTypedValue("typedValueForStringList", strings, APPLICATION_JAVA, UTF8);
  }

  @Test
  public void typedValueForStringListAsChild() throws Exception {
    List<Object> strings = new ArrayList<>();
    strings.add("string");
    strings.add("string");
    runAndAssertTypedValue("typedValueForStringListAsChild", strings, MediaType.ANY, null);
  }

  @Test
  public void typedValueForStringMap() throws Exception {
    HashMap<Object, Object> map = new HashMap<>();
    map.put("string", "string");
    runAndAssertTypedValue("typedValueForStringMap", map, APPLICATION_JAVA, UTF8);
  }

  @Test
  public void typedValueForStringMapAsChild() throws Exception {
    HashMap<Object, Object> map = new LinkedHashMap<>();
    map.put("string", "string");
    runAndAssertTypedValue("typedValueForStringMapAsChild", map, MediaType.ANY, null);
  }

  @Test
  public void typedValueForDoorAsChild() throws Exception {
    runAndAssertTypedValue("typedValueForDoorAsChild", DOOR, MediaType.ANY, null);
  }

  @Test
  public void typedValueForDoorListAsChild() throws Exception {
    ArrayList<Object> doors = new ArrayList<>();
    doors.add(DOOR);
    runAndAssertTypedValue("typedValueForDoorListAsChild", doors, MediaType.ANY, null);
  }

  @Test
  public void typedValueForDoorMapAsChild() throws Exception {
    Map<Object, Object> doors = new LinkedHashMap<>();
    doors.put("key", DOOR);
    runAndAssertTypedValue("typedValueForDoorMapAsChild", doors, MediaType.ANY, null);
  }

  @Test
  public void typedValueOperationStringMapListParameter() throws Exception {
    Map<Object, Object> mapStringList = new LinkedHashMap<>();
    mapStringList.put("key", Collections.singletonList("string"));
    runAndAssertTypedValue("typedValueOperationStringMapListParameter", mapStringList, MediaType.ANY, null);
  }

  @Test
  public void typedValueForStringOnSourceOnSuccess() throws Exception {
    Flow flow = (Flow) getFlowConstruct("typedValueForStringOnSourceOnSuccess");
    flow.start();
    new PollingProber().check(new JUnitLambdaProbe(() -> TypedValueSource.onSuccessValue != null));
    assertTypedValue(TypedValueSource.onSuccessValue, "string", MediaType.APPLICATION_JSON, UTF8);
  }

  @Test
  public void typedValueForStringInsidePojo() throws Exception {
    Event event = flowRunner("typedValueForStringInsidePojo").run();
    DifferedKnockableDoor value = (DifferedKnockableDoor) event.getMessage().getPayload().getValue();
    assertTypedValue(value.getAddress(), "string", MediaType.APPLICATION_JSON, UTF8);
  }
}
