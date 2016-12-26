/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.junit.After;
import org.junit.Test;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypedValueParameterOperationExecutionTestCase extends ExtensionFunctionalTestCase {

  private static final MediaType APPLICATION_JAVA = MediaType.parse("application/java");
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final KnockeableDoor DOOR = new KnockeableDoor("Saul");
  private static final String THIS_IS_A_DEFAULT_STRING = "This is a default string";

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

  private void runAndAssertTypedValue(String flowName, Object payloadValue, MediaType mediaType, Charset charset)
      throws Exception {
    Object payload = flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertTypedValue((TypedValue) payload, payloadValue, mediaType, charset);
  }

  private void assertTypedValue(TypedValue typedValue, Object payloadValue, MediaType mediaType, Charset charset) {
    assertThat(typedValue, is(instanceOf(TypedValue.class)));
    Object value = typedValue.getValue();
    assertThat(value, is(instanceOf(payloadValue.getClass())));
    assertThat(value, is(payloadValue));
    assertThat(typedValue.getDataType(), is(like(payloadValue.getClass(), mediaType, charset)));
  }


  @Operations(TypedValueParameterOperations.class)
  @Extension(name = "TypedValue")
  @Sources(TypedValueSource.class)
  public static class TypedValueExtension {

  }

  public static class TypedValueParameterOperations {

    public TypedValue<String> typedValueOperationStringParameter(@Optional(
        defaultValue = THIS_IS_A_DEFAULT_STRING) TypedValue<String> stringValue,
                                                                 @UseConfig TypedValueExtension config) {
      return stringValue;
    }

    public TypedValue<List<String>> typedValueOperationStringListParameter(TypedValue<List<String>> stringValues) {
      return stringValues;
    }

    public TypedValue<Map<String, String>> typedValueOperationStringMapParameter(TypedValue<Map<String, String>> stringMapValues) {
      return stringMapValues;
    }

    public TypedValue<KnockeableDoor> typedValueOperationDoorParameter(TypedValue<KnockeableDoor> doorValue) {
      return doorValue;
    }

    public TypedValue<List<KnockeableDoor>> typedValueOperationDoorListParameter(TypedValue<List<KnockeableDoor>> doorValues) {
      return doorValues;
    }

    public TypedValue<Map<String, KnockeableDoor>> typedValueOperationDoorMapParameter(TypedValue<Map<String, KnockeableDoor>> doorMapValues) {
      return doorMapValues;
    }

    public TypedValue<Map<String, List<String>>> typedValueOperationStringMapListParameter(TypedValue<Map<String, List<String>>> doorMapListValues) {
      return doorMapListValues;
    }
  }

  @Alias("source")
  public static class TypedValueSource extends Source<String, Attributes> {

    public static TypedValue<String> onSuccessValue;

    @Override
    public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {
      sourceCallback.handle(Result.<String, Attributes>builder().output("This is a string").build());
    }

    @Override
    public void onStop() {

    }

    @OnSuccess
    public void onSuccess(TypedValue<String> stringValue) {
      onSuccessValue = stringValue;
    }
  }
}
