/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.typed.value;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public abstract class AbstractTypedValueTestCase extends ExtensionFunctionalTestCase {

  static final String THIS_IS_A_DEFAULT_STRING = "This is a default string";
  static final MediaType APPLICATION_JAVA = MediaType.parse("application/java");
  static final Charset UTF8 = Charset.forName("UTF-8");
  static final KnockeableDoor DOOR = new KnockeableDoor("Saul");

  void runAndAssertTypedValue(String flowName, Object payloadValue, MediaType mediaType, Charset charset)
      throws Exception {
    Object payload = flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertTypedValue((TypedValue) payload, payloadValue, mediaType, charset);
  }

  void assertTypedValue(TypedValue typedValue, Object payloadValue, MediaType mediaType, Charset charset) {
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

    @Parameter
    @Optional
    TypedValue<String> stringTypedValue;

    @Parameter
    @Optional
    DifferedKnockableDoor differedDoor;

    public TypedValue<String> getStringTypedValue() {
      return stringTypedValue;
    }

    public DifferedKnockableDoor getDifferedDoor() {
      return differedDoor;
    }
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

    public DifferedKnockableDoor typedValueForStringInsidePojo(DifferedKnockableDoor differedDoor) {
      return differedDoor;
    }

    public TypedValueExtension typedValueOnConfig(@UseConfig TypedValueExtension extension) {
      return extension;
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
