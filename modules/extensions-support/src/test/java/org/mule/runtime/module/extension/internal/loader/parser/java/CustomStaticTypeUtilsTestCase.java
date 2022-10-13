/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getOperationAttributesType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getOperationOutputType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getParameterType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getSourceAttributesType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.type.CustomStaticTypeUtils.getSourceOutputType;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.resolving.AttributesStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.Test;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

public class CustomStaticTypeUtilsTestCase {

  private static final String TEST_PARAMETER_METHOD = "testParameterMethod";
  private static final MetadataType CUSTOM_STATIC_METADATATYPE =
      BaseTypeBuilder.create(MetadataFormat.JAVA).objectType().id("custom-java").build();

  @Test
  public void xmlInputStaticType() throws Exception {
    assertXmlType(getParameterType(getExtensionParameter("xmlInputStaticType")));
  }

  @Test
  public void jsonInputStaticType() throws Exception {
    assertJsonType(getParameterType(getExtensionParameter("jsonInputStaticType")));
  }

  @Test
  public void customInputStaticType() throws Exception {
    assertCustomType(getParameterType(getExtensionParameter("customInputStaticType")));
  }

  @Test
  public void xmlOutputStaticType() throws Exception {
    assertXmlType(getOperationOutputType(getOperationElementMethodName("xmlOutputStaticTypeOperation")));
  }

  @Test
  public void jsonOutputStaticType() throws Exception {
    assertJsonType(getOperationOutputType(getOperationElementMethodName("jsonOutputStaticTypeOperation")));
  }

  @Test
  public void customOutputStaticType() throws Exception {
    assertCustomType(getOperationOutputType(getOperationElementMethodName("customOutputStaticTypeOperation")));
  }

  @Test
  public void xmlAttributesStaticType() throws Exception {
    assertXmlType(getOperationAttributesType(getOperationElementMethodName("xmlAttributeStaticTypeOperation")));
  }

  @Test
  public void jsonAttributesStaticType() throws Exception {
    assertJsonType(getOperationAttributesType(getOperationElementMethodName("jsonAttributesStaticTypeOperation")));
  }

  @Test
  public void customAttributesStaticType() throws Exception {
    assertCustomType(getOperationAttributesType(getOperationElementMethodName("customAttributesStaticTypeOperation")));
  }

  @Test
  public void xmlOutputStaticTypeSource() {
    assertXmlType(getSourceOutputType(getSourceElementWithClass(XmlOutputStaticTypeSource.class)));
  }

  @Test
  public void jsonOutputStaticTypeSource() {
    assertJsonType(getSourceOutputType(getSourceElementWithClass(JsonOutputStaticTypeSource.class)));
  }

  @Test
  public void customOutputStaticTypeSource() {
    assertCustomType(getSourceOutputType(getSourceElementWithClass(CustomOutputStaticTypeSource.class)));
  }

  @Test
  public void xmlAttributesStaticTypeSource() {
    assertXmlType(getSourceAttributesType(getSourceElementWithClass(XmlAttributesStaticTypeSource.class)));
  }

  @Test
  public void jsonAttributesStaticTypeSource() {
    assertJsonType(getSourceAttributesType(getSourceElementWithClass(JsonAttributesStaticTypeSource.class)));
  }

  @Test
  public void customAttributesStaticTypeSource() {
    assertCustomType(getSourceAttributesType(getSourceElementWithClass(CustomAttributesStaticTypeSource.class)));
  }

  @Test
  public void sdkXmlInputStaticType() throws Exception {
    assertXmlType(getParameterType(getExtensionParameter("sdkXmlInputStaticType")));
  }

  @Test
  public void sdkJsonInputStaticType() throws Exception {
    assertJsonType(getParameterType(getExtensionParameter("sdkJsonInputStaticType")));
  }

  @Test
  public void sdkCustomInputStaticType() throws Exception {
    assertCustomType(getParameterType(getExtensionParameter("sdkCustomInputStaticType")));
  }

  @Test
  public void sdkXmlOutputStaticType() throws Exception {
    assertXmlType(getOperationOutputType(getOperationElementMethodName("sdkXmlOutputStaticTypeOperation")));
  }

  @Test
  public void sdkJsonOutputStaticType() throws Exception {
    assertJsonType(getOperationOutputType(getOperationElementMethodName("sdkJsonOutputStaticTypeOperation")));
  }

  @Test
  public void sdkCustomOutputStaticType() throws Exception {
    assertCustomType(getOperationOutputType(getOperationElementMethodName("sdkCustomOutputStaticTypeOperation")));
  }

  @Test
  public void sdkXmlAttributesStaticType() throws Exception {
    assertXmlType(getOperationAttributesType(getOperationElementMethodName("sdkXmlAttributeStaticTypeOperation")));
  }

  @Test
  public void sdkJsonAttributesStaticType() throws Exception {
    assertJsonType(getOperationAttributesType(getOperationElementMethodName("sdkJsonAttributesStaticTypeOperation")));
  }

  @Test
  public void sdkCustomAttributesStaticType() throws Exception {
    assertCustomType(getOperationAttributesType(getOperationElementMethodName("sdkCustomAttributesStaticTypeOperation")));
  }

  @Test
  public void sdkXmlOutputStaticTypeSource() {
    assertXmlType(getSourceOutputType(getSourceElementWithClass(SdkXmlOutputStaticTypeSource.class)));
  }

  @Test
  public void sdkJsonOutputStaticTypeSource() {
    assertJsonType(getSourceOutputType(getSourceElementWithClass(SdkJsonOutputStaticTypeSource.class)));
  }

  @Test
  public void sdkCustomOutputStaticTypeSource() {
    assertCustomType(getSourceOutputType(getSourceElementWithClass(SdkCustomOutputStaticTypeSource.class)));
  }

  @Test
  public void sdkXmlAttributesStaticTypeSource() {
    assertXmlType(getSourceAttributesType(getSourceElementWithClass(SdkXmlAttributesStaticTypeSource.class)));
  }

  @Test
  public void sdkJsonAttributesStaticTypeSource() {
    assertJsonType(getSourceAttributesType(getSourceElementWithClass(SdkJsonAttributesStaticTypeSource.class)));
  }

  @Test
  public void sdkCustomAttributesStaticTypeSource() {
    assertCustomType(getSourceAttributesType(getSourceElementWithClass(SdkCustomAttributesStaticTypeSource.class)));
  }

  @Test
  public void enumParameterType() throws NoSuchMethodException {
    TypeWrapper type = new TypeWrapper(TestEnum.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    for (ExtensionParameter parameter : type.getMethod("compareTo", Enum.class).map(WithParameters::getParameters)
        .orElse(emptyList())) {
      parameter.getType();
    }
  }

  private SourceElement getSourceElementWithClass(Class<? extends Source> sourceClass) {
    return new SourceTypeWrapper<>(sourceClass, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
  }

  private OperationElement getOperationElementMethodName(String methodName) throws Exception {
    return new OperationWrapper(getMethod(methodName), new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
  }

  private ExtensionParameter getExtensionParameter(String parameterName) throws Exception {
    Method parametersMethod = getMethod(TEST_PARAMETER_METHOD);
    return new ParameterWrapper(parametersMethod, getParameterIndex(parameterName, parametersMethod),
                                new DefaultExtensionsTypeLoaderFactory()
                                    .createTypeLoader(Thread.currentThread().getContextClassLoader()));
  }



  private Method getMethod(String methodName) throws NoSuchMethodException {
    return stream(getClass().getMethods()).filter(method -> method.getName().equals(methodName)).findFirst().get();
  }

  private int getParameterIndex(String parameterName, Method method) {
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getName().equals(parameterName)) {
        return i;
      }
    }
    throw new IllegalArgumentException("No parameter for the given arguments");
  }

  private void assertXmlType(MetadataType type) {
    assertThat(type, instanceOf(ObjectType.class));

    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(1));
    assertThat(objectType.getFieldByName("shiporder").isPresent(), is(true));

    assertThat(type.getAnnotation(CustomDefinedStaticTypeAnnotation.class).isPresent(), is(true));
    assertThat(type.getAnnotation(ClassInformationAnnotation.class).get().getClassname(), is(String.class.getName()));
    assertThat(type.getAnnotation(TypeIdAnnotation.class).get().getValue(), is("#root:shiporder"));
  }

  private void assertJsonType(MetadataType type) {
    assertThat(type, instanceOf(ObjectType.class));

    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(3));
    assertThat(objectType.getFieldByName("firstName").isPresent(), is(true));

    assertThat(type.getAnnotation(CustomDefinedStaticTypeAnnotation.class).isPresent(), is(true));
    assertThat(type.getAnnotation(ClassInformationAnnotation.class).get().getClassname(), is(String.class.getName()));
    assertThat(type.getAnnotation(TypeIdAnnotation.class).get().getValue(), is("http://example.com/example.json"));
  }


  private void assertCustomType(MetadataType type) {
    assertThat(type.getMetadataFormat(), is(MetadataFormat.JAVA));
    assertThat(type, instanceOf(ObjectType.class));

    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(0));

    assertThat(type.getAnnotation(CustomDefinedStaticTypeAnnotation.class).isPresent(), is(true));
    assertThat(type.getAnnotation(ClassInformationAnnotation.class).get().getClassname(), is(String.class.getName()));
    assertThat(type.getAnnotation(TypeIdAnnotation.class).get().getValue(), is("custom-java"));
  }

  public void testParameterMethod(@InputXmlType(schema = "order.xsd", qname = "shiporder") String xmlInputStaticType,
                                  @org.mule.sdk.api.annotation.metadata.fixed.InputXmlType(schema = "order.xsd",
                                      qname = "shiporder") String sdkXmlInputStaticType,
                                  @InputJsonType(schema = "person-schema.json") String jsonInputStaticType,
                                  @org.mule.sdk.api.annotation.metadata.fixed.InputJsonType(
                                      schema = "person-schema.json") String sdkJsonInputStaticType,
                                  @TypeResolver(TestInputStaticTypeResolver.class) String customInputStaticType,
                                  @org.mule.sdk.api.annotation.metadata.TypeResolver(SdkTestInputStaticTypeResolver.class) String sdkCustomInputStaticType) {

  }

  @OutputXmlType(schema = "order.xsd", qname = "shiporder")
  public String xmlOutputStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.fixed.OutputXmlType(schema = "order.xsd", qname = "shiporder")
  public String sdkXmlOutputStaticTypeOperation() {
    return null;
  }

  @OutputJsonType(schema = "person-schema.json")
  public String jsonOutputStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.fixed.OutputJsonType(schema = "person-schema.json")
  public String sdkJsonOutputStaticTypeOperation() {
    return null;
  }

  @OutputResolver(output = TestOutputStaticTypeResolver.class)
  public String customOutputStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.OutputResolver(output = SdkTestOutputStaticTypeResolver.class)
  public String sdkCustomOutputStaticTypeOperation() {
    return null;
  }

  @AttributesXmlType(schema = "order.xsd", qname = "shiporder")
  public Result<String, String> xmlAttributeStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.fixed.AttributesXmlType(schema = "order.xsd", qname = "shiporder")
  public Result<String, String> sdkXmlAttributeStaticTypeOperation() {
    return null;
  }

  @AttributesJsonType(schema = "person-schema.json")
  public Result<String, String> jsonAttributesStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.fixed.AttributesJsonType(schema = "person-schema.json")
  public Result<String, String> sdkJsonAttributesStaticTypeOperation() {
    return null;
  }

  @OutputResolver(attributes = TestAttributesStaticTypeResolver.class)
  public Result<String, String> customAttributesStaticTypeOperation() {
    return null;
  }

  @org.mule.sdk.api.annotation.metadata.OutputResolver(attributes = SdkTestAttributesStaticTypeResolver.class)
  public Result<String, String> sdkCustomAttributesStaticTypeOperation() {
    return null;
  }

  private static class BaseSource extends Source<String, String> {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @OutputXmlType(schema = "order.xsd", qname = "shiporder")
  private static class XmlOutputStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.fixed.OutputXmlType(schema = "order.xsd", qname = "shiporder")
  private static class SdkXmlOutputStaticTypeSource extends BaseSource {

  }

  @OutputJsonType(schema = "person-schema.json")
  private static class JsonOutputStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.fixed.OutputJsonType(schema = "person-schema.json")
  private static class SdkJsonOutputStaticTypeSource extends BaseSource {

  }

  @MetadataScope(outputResolver = TestOutputStaticTypeResolver.class)
  private static class CustomOutputStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.MetadataScope(outputResolver = SdkTestOutputStaticTypeResolver.class)
  private static class SdkCustomOutputStaticTypeSource extends BaseSource {

  }

  @AttributesXmlType(schema = "order.xsd", qname = "shiporder")
  private static class XmlAttributesStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.fixed.AttributesXmlType(schema = "order.xsd", qname = "shiporder")
  private static class SdkXmlAttributesStaticTypeSource extends BaseSource {

  }

  @AttributesJsonType(schema = "person-schema.json")
  private static class JsonAttributesStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.fixed.AttributesJsonType(schema = "person-schema.json")
  private static class SdkJsonAttributesStaticTypeSource extends BaseSource {

  }

  @MetadataScope(attributesResolver = TestAttributesStaticTypeResolver.class)
  private static class CustomAttributesStaticTypeSource extends BaseSource {

  }

  @org.mule.sdk.api.annotation.metadata.MetadataScope(attributesResolver = SdkTestAttributesStaticTypeResolver.class)
  private static class SdkCustomAttributesStaticTypeSource extends BaseSource {

  }

  public static class TestOutputStaticTypeResolver extends OutputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }

  }

  public static class TestInputStaticTypeResolver extends InputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }

  }

  public static class TestAttributesStaticTypeResolver extends AttributesStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }

  }

  public static class SdkTestOutputStaticTypeResolver extends org.mule.sdk.api.metadata.resolving.OutputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }

  }

  public static class SdkTestInputStaticTypeResolver extends org.mule.sdk.api.metadata.resolving.InputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }

  }

  public static class SdkTestAttributesStaticTypeResolver
      extends org.mule.sdk.api.metadata.resolving.AttributesStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
      return CUSTOM_STATIC_METADATATYPE;
    }
  }

  public enum TestEnum {
    SOME_VALUE, ANOTHER_VALUE
  }
}
