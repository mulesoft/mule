/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.typed.value;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.test.typed.value.extension.extension.TypedValueParameterOperations.THIS_IS_A_DEFAULT_STRING;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.typed.value.extension.extension.SimplePojo;
import org.mule.test.typed.value.extension.extension.TypedValueSource;
import org.mule.test.vegan.extension.VeganProductInformation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

public class TypedValueParameterOperationExecutionTestCase extends AbstractTypedValueTestCase {

  private static final String STRING_VALUE = "string";
  private static final String JSON_OBJECT = "{\n" +
      "  \"a\": \"json value\"\n" +
      "}";
  private static final String THIS_IS_A_STRING = "This is a string";

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"typed-value-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @After
  public void cleanUp() {
    TypedValueSource.onSuccessValue = null;
  }

  @Test
  public void typedValueForString() throws Exception {
    runAndAssertTypedValue("typedValueForString", THIS_IS_A_STRING, WILDCARD, null);
  }

  @Test
  public void typedValueForStringFromByteArray() throws Exception {
    runAndAssertTypedValue("typedValueForStringFromByteArray", THIS_IS_A_STRING, WILDCARD, null);
  }

  @Test
  public void typedValueForStringWithDefaultValue() throws Exception {
    runAndAssertTypedValue("typedValueForStringWithDefaultValue", THIS_IS_A_DEFAULT_STRING, ANY, null);
  }

  @Test
  public void typedValueForStringList() throws Exception {
    List<Object> strings = new ArrayList<>();
    strings.add("string");
    strings.add("string");
    runAndAssertTypedValue("typedValueForStringList", strings, WILDCARD, null);
  }

  @Test
  public void typedValueForStringListAsChild() throws Exception {
    List<Object> strings = new ArrayList<>();
    strings.add("string");
    strings.add("string");
    runAndAssertTypedValue("typedValueForStringListAsChild", strings, ANY, null);
  }

  @Test
  public void typedValueForStringMap() throws Exception {
    HashMap<Object, Object> map = new LinkedHashMap<>();
    map.put("string", "string");
    runAndAssertTypedValue("typedValueForStringMap", map, WILDCARD, null);
  }

  @Test
  public void typedValueForStringMapAsChild() throws Exception {
    HashMap<Object, Object> map = new LinkedHashMap<>();
    map.put("string", "string");
    runAndAssertTypedValue("typedValueForStringMapAsChild", map, ANY, null);
  }

  @Test
  public void typedValueForDoorAsChild() throws Exception {
    runAndAssertTypedValue("typedValueForDoorAsChild", DOOR, ANY, null);
  }

  @Test
  public void typedValueForDoorListAsChild() throws Exception {
    ArrayList<Object> doors = new ArrayList<>();
    doors.add(DOOR);
    runAndAssertTypedValue("typedValueForDoorListAsChild", doors, ANY, null);
  }

  @Test
  public void typedValueForDoorMapAsChild() throws Exception {
    Map<Object, Object> doors = new LinkedHashMap<>();
    doors.put("key", DOOR);
    runAndAssertTypedValue("typedValueForDoorMapAsChild", doors, ANY, null);
  }

  @Test
  public void typedValueOperationStringMapListParameter() throws Exception {
    Map<Object, Object> mapStringList = new LinkedHashMap<>();
    mapStringList.put("key", Collections.singletonList("string"));
    runAndAssertTypedValue("typedValueOperationStringMapListParameter", mapStringList, ANY, null);
  }

  @Test
  public void typedValueForStringOnSourceOnSuccess() throws Exception {
    Flow flow = (Flow) getFlowConstruct("typedValueForStringOnSourceOnSuccess");
    flow.start();
    new PollingProber(100000, 100).check(new JUnitLambdaProbe(() -> TypedValueSource.onSuccessValue != null));
    assertTypedValue(TypedValueSource.onSuccessValue, STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueForStringInsidePojo() throws Exception {
    CoreEvent event = flowRunner("typedValueForStringInsidePojo").run();
    DifferedKnockableDoor value = (DifferedKnockableDoor) event.getMessage().getPayload().getValue();
    assertTypedValue(value.getAddress(), STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueForContentOnNullSafePojoWithDefaultValue() throws Exception {
    CoreEvent event = flowRunner("typedValueForContentOnNullSafePojoWithDefaultValue").run();
    VeganProductInformation value = (VeganProductInformation) event.getMessage().getPayload().getValue();
    assertTypedValue(value.getDescription(), STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueForContentOnNullSafePojoWithDefaultValueWithOutDefiningPojo() throws Exception {
    CoreEvent event = flowRunner("typedValueForContentOnNullSafePojoWithDefaultValueWithOutDefiningPojo").run();
    VeganProductInformation value = (VeganProductInformation) event.getMessage().getPayload().getValue();
    assertTypedValue(value.getDescription(), STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueOnContentOnNullSafeWithExplicitValues() throws Exception {
    CoreEvent event = flowRunner("typedValueOnContentOnNullSafeWithExplicitValues").run();
    VeganProductInformation value = (VeganProductInformation) event.getMessage().getPayload().getValue();
    assertTypedValue(value.getDescription(), STRING_VALUE, WILDCARD, null);
    assertTypedValue(value.getBrandName(), STRING_VALUE, WILDCARD, null);
    assertTypedValue(value.getWeight(), 5, WILDCARD, null);
  }

  @Test
  public void typedValueForObject() throws Exception {
    CoreEvent event = flowRunner("typedValueForObject").keepStreamsOpen().run();
    TypedValue jsonObject = (TypedValue) event.getMessage().getPayload().getValue();
    InputStream content = (InputStream) jsonObject.getValue();
    assertThat(IOUtils.toString(content), is(JSON_OBJECT));
    assertThat(jsonObject.getDataType(), is(like(jsonObject.getDataType().getType(), APPLICATION_JSON, UTF8)));
  }

  @Test
  public void typedValueForInputStream() throws Exception {
    CoreEvent event = flowRunner("typedValueForInputStream").run();
    TypedValue jsonObject = (TypedValue) event.getMessage().getPayload().getValue();
    assertThat(IOUtils.toString((InputStream) jsonObject.getValue()), is(JSON_OBJECT));
    assertThat(jsonObject.getDataType(), is(like(jsonObject.getDataType().getType(), APPLICATION_JSON, UTF8)));
  }

  @Test
  public void typedValueOperationWithExplicitStringContent() throws Exception {
    runAndAssertTypedValue("typedValueOperationWithExplicitStringContent", STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueOperationWithDefaultStringContent() throws Exception {
    runAndAssertTypedValue("typedValueOperationWithDefaultStringContent", STRING_VALUE, WILDCARD, null);
  }

  @Test
  public void typedValueOperationWithExplicitNullContent() throws Exception {
    runAndAssertTypedValue("typedValueOperationWithExplicitNullContent", null, APPLICATION_JSON, UTF8);
  }

  @Test
  public void wrappedAndUnwrappedTypes() throws Exception {
    List<Object> wrappedAndUnwrappedTypes = (List<Object>) flowRunner("wrappedAndUnwrappedTypes").run()
        .getMessage().getPayload().getValue();
    assertThat(wrappedAndUnwrappedTypes.get(0), is("stringNotWrapped"));
    assertThat(((TypedValue) wrappedAndUnwrappedTypes.get(1)).getValue(), is("wrappedString"));
    assertThat(((SimplePojo) ((TypedValue) wrappedAndUnwrappedTypes.get(2)).getValue()).getUser(), is("user"));
    assertThat(((SimplePojo) wrappedAndUnwrappedTypes.get(3)).getUser(), is("user2"));

    Map<String, Object> mapOfComplexValues = (Map<String, Object>) wrappedAndUnwrappedTypes.get(4);
    Map<String, TypedValue<?>> mapOfComplexTypedValues = (Map<String, TypedValue<?>>) wrappedAndUnwrappedTypes.get(5);
    assertThat(mapOfComplexValues.entrySet().size(), is(2));
    assertThat(((Map) mapOfComplexValues.get("first")).get("pass"), is("pass"));
    assertThat(mapOfComplexTypedValues.entrySet().size(), is(1));
    assertThat(((Map) ((TypedValue) mapOfComplexTypedValues.get("third")).getValue()).get("pass"), is("pass3"));

    SimplePojo group = (SimplePojo) wrappedAndUnwrappedTypes.get(6);
    assertThat(group.getUser(), is("groupUser"));
    assertThat(group.getPass(), is("groupPass"));
  }
}
