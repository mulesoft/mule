/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.plain;
import static org.mule.runtime.module.tooling.internal.artifact.params.ParameterExtractor.asDataWeaveExpression;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;

import org.junit.Test;

public class ParameterExtractorTestCase {

  @Test
  public void dateTimeTypeAsDateTime() {
    final String param = "my parameter";
    MetadataType dateTimeType = new BaseTypeBuilder(JAVA).dateTimeType().build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), dateTimeType);
    checkContains(extracted, "as DateTime");
  }

  @Test
  public void dateTypeAsDate() {
    final String param = "my parameter";
    MetadataType dateType = new BaseTypeBuilder(JAVA).dateType().build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), dateType);
    checkContains(extracted, "as Date");
  }

  @Test
  public void localDateTimeTypeAsLocalDateTime() {
    final String param = "my parameter";
    MetadataType localDateTimeType = new BaseTypeBuilder(JAVA).localDateTimeType().build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), localDateTimeType);
    checkContains(extracted, "as LocalDateTime");
  }

  @Test
  public void localTimeTypeAsLocalTime() {
    final String param = "my parameter";
    MetadataType localTimeType = new BaseTypeBuilder(JAVA).localTimeType().build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), localTimeType);
    checkContains(extracted, "as LocalTime");
  }

  @Test
  public void javaUtilDateAsDateTime() {
    final String param = "my parameter";
    MetadataType dateTimeType =
        new BaseTypeBuilder(JAVA).dateType().with(new TypeIdAnnotation(java.util.Date.class.getName())).build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), dateTimeType);
    checkContains(extracted, "as DateTime");
  }

  @Test
  public void javaLocalDateTimeAsLocalDateTime() {
    final String param = "my parameter";
    MetadataType dateTimeType =
        new BaseTypeBuilder(JAVA).dateTimeType().with(new TypeIdAnnotation(java.time.LocalDateTime.class.getName())).build();
    TypedValue<?> extracted = asDataWeaveExpression(plain(param), dateTimeType);
    checkContains(extracted, "as LocalDateTime");
  }

  @Test
  public void notMatchingTypeAndDeclarationStillWorks() {
    MetadataType metadataType = new BaseTypeBuilder(JAVA).stringType().build();
    ParameterValue level0 = ParameterObjectValue.builder().withParameter("field", plain("value")).build();
    ParameterValue parameterValue = ParameterObjectValue.builder().withParameter("level0", level0).build();
    TypedValue<?> extracted = asDataWeaveExpression(parameterValue, metadataType);
    checkContains(extracted, "{\"level0\":{\"field\":\"value\"}}");
  }

  @Test
  public void arrayOfDate() {
    final String date = "my date";
    MetadataType dateType = new BaseTypeBuilder(JAVA).dateType().build();
    MetadataType metadataType = new BaseTypeBuilder(JAVA).arrayType().of(dateType).build();
    ParameterValue parameterValue = ParameterListValue.builder().withValue(date).build();
    TypedValue<?> extracted = asDataWeaveExpression(parameterValue, metadataType);
    checkContains(extracted, "as Date");
  }

  @Test
  public void objectOfDate() {
    final String date = "my date";
    MetadataType dateType = new BaseTypeBuilder(JAVA).dateType().build();
    ObjectTypeBuilder metadataTypeBuilder = new BaseTypeBuilder(JAVA).objectType();
    metadataTypeBuilder.addField().key("myDate").value(dateType);
    ParameterValue parameterValue = ParameterObjectValue.builder().withParameter("myDate", date).build();
    TypedValue<?> extracted = asDataWeaveExpression(parameterValue, metadataTypeBuilder.build());
    checkContains(extracted, "as Date");
  }

  private void checkContains(TypedValue<?> typedValue, String content) {
    String value = (String) typedValue.getValue();
    assertThat(value, containsString(content));
  }

}
