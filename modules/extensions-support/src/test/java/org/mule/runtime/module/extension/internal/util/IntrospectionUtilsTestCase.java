/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.dictionaryOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;

import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.builder.StringTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBox;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.test.petstore.extension.PhoneNumber;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.Test;
import org.springframework.core.ResolvableType;

@SmallTest
public class IntrospectionUtilsTestCase extends AbstractMuleTestCase {

  public static final String OPERATION_RESULT = "operationResult";
  public static final String PAGING_PROVIDER = "pagingProvider";
  public static final String PAGING_PROVIDER_OPERATION_RESULT = "pagingProviderOperationResult";
  public static final String FOO = "foo";
  private List<FruitBasket> baskets;

  @Test
  public void getMethodReturnType() throws Exception {
    MetadataType metadataType = IntrospectionUtils.getMethodReturnType(getMethod(FOO), TYPE_LOADER);
    assertDictionary(metadataType, Apple.class);
  }

  @Test
  public void getOperationResultReturnType() throws Exception {
    assertReturnType(OPERATION_RESULT);
    assertAttributesType(OPERATION_RESULT);
  }

  @Test
  public void getPagingProviderReturnType() throws Exception {
    assertPagingProviderReturnType(PAGING_PROVIDER);
    assertVoidAttributesType(PAGING_PROVIDER);
  }

  @Test
  public void getPagingProviderOperationResultReturnType() throws Exception {
    assertPagingProviderReturnResultType((PAGING_PROVIDER_OPERATION_RESULT));
    assertVoidAttributesType(PAGING_PROVIDER_OPERATION_RESULT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNullMethodReturnType() throws Exception {
    IntrospectionUtils.getMethodReturnType(null, TYPE_LOADER);
  }

  @Test
  public void getArgumentlessMethodArgumentTypes() throws Exception {
    MetadataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod(FOO), TYPE_LOADER);
    assertNotNull(types);
    assertEquals(0, types.length);
  }

  @Test
  public void getMethodArgumentTypes() throws Exception {
    MetadataType[] types = IntrospectionUtils
        .getMethodArgumentTypes(getMethod("bar", String.class, Long.class, Apple.class, Map.class), TYPE_LOADER);
    assertNotNull(types);
    assertEquals(4, types.length);

    assertType(types[0], String.class);
    assertType(types[1], Long.class);
    assertType(types[2], Apple.class);
    assertDictionary(types[3], Kiwi.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNullMethodArgumentTypes() throws Exception {
    IntrospectionUtils.getMethodArgumentTypes(null, TYPE_LOADER);
  }

  @Test
  public void getFieldDataType() throws Exception {
    MetadataType type = getFieldMetadataType(getClass().getDeclaredField("baskets"), TYPE_LOADER);
    assertList(type, FruitBasket.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNullFieldDataType() throws Exception {
    getFieldMetadataType(null, TYPE_LOADER);
  }

  @Test
  public void getEmptyExposedPojoFields() {
    Collection<Field> exposedFields = getExposedFields(FruitBasket.class);
    assertThat(exposedFields, is(empty()));
  }

  @Test
  public void getFieldsWithGettersOnly() {
    Collection<Field> fieldsWithGetters = getFieldsWithGetters(PhoneNumber.class);
    assertThat(fieldsWithGetters.size(), is(4));
  }

  @Test
  public void getWildCardFieldsDataTypes() {

    Collection<Field> exposedFields = getFieldsWithGetters(FruitBox.class);
    assertNotNull(exposedFields);
    assertEquals(6, exposedFields.size());
    assertField("fruitLikeList", arrayOf(List.class, objectTypeBuilder(Fruit.class)), exposedFields);
    assertField("wildCardList", arrayOf(List.class, objectTypeBuilder(Object.class)), exposedFields);
    assertField("rawList", arrayOf(List.class, objectTypeBuilder(Object.class)), exposedFields);
    assertField("wildCardMap", dictionaryOf(objectTypeBuilder(Object.class)),
                exposedFields);
    assertField("rawMap", dictionaryOf(objectTypeBuilder(Object.class)),
                exposedFields);
    assertField("fruitLikeMap", dictionaryOf(objectTypeBuilder(Fruit.class)),
                exposedFields);
  }

  @Test
  public void getDataTypeFromList() {
    Class<List> listClass = List.class;
    Class<Integer> integerClass = Integer.class;

    ArrayTypeBuilder arrayTypeBuilder = BaseTypeBuilder.create(JAVA)
        .arrayType()
        .with(new ClassInformationAnnotation(listClass));
    arrayTypeBuilder.of().numberType().integer();

    CollectionDataType dataType = (CollectionDataType) toDataType(arrayTypeBuilder.build());

    assertThat(dataType.getType(), is(equalTo(listClass)));
    assertThat(dataType.getItemDataType().getType(), is(equalTo(integerClass)));
  }

  @Test
  public void getDataTypeFromMap() {
    Class<Date> dateClass = Date.class;
    Class<Map> mapClass = Map.class;

    ObjectTypeBuilder objectTypeBuilder = BaseTypeBuilder
        .create(JAVA)
        .objectType()
        .with(new ClassInformationAnnotation(Map.class));

    objectTypeBuilder.openWith().objectType().id(dateClass.getName());

    MapDataType dataType = (MapDataType) toDataType(objectTypeBuilder.build());

    assertThat(dataType.getType(), is(equalTo(mapClass)));
    assertThat(dataType.getKeyDataType().getType(), is(equalTo(String.class)));
    assertThat(dataType.getValueDataType().getType(), is(equalTo(dateClass)));
  }

  @Test
  public void getDataTypeFromObject() {
    Class<Object> objectClass = Object.class;

    ObjectTypeBuilder objectTypeBuilder = BaseTypeBuilder
        .create(JAVA)
        .objectType()
        .id(objectClass.getName());

    DataType dataType = toDataType(objectTypeBuilder.build());

    assertThat(dataType.getType(), is(equalTo(objectClass)));
  }

  @Test
  public void getDataTypeFromString() {
    StringTypeBuilder typeBuilder = BaseTypeBuilder
        .create(JAVA)
        .stringType();

    DataType dataType = toDataType(typeBuilder.build());

    assertThat(dataType.getType(), is(equalTo(String.class)));
  }

  @Test
  public void getPagingProviderImplementationTypes() {
    ResolvableType pagingProvider = ResolvableType.forClass(TestPagingProvider.class);
    Pair<ResolvableType, ResolvableType> pagingProviderTypes = IntrospectionUtils.getPagingProviderTypes(pagingProvider);

    assertThat(pagingProviderTypes.getFirst().getRawClass(), equalTo(Object.class));
    assertThat(pagingProviderTypes.getSecond().getRawClass(), equalTo((Banana.class)));
  }

  private void assertField(String name, MetadataType metadataType, Collection<Field> fields) {
    Field field = findField(name, fields);
    assertThat(field, is(notNullValue()));
    assertThat(field.getName(), equalTo(name));
    assertThat(field.getType(), equalTo(JavaTypeUtils.getType(metadataType)));
  }

  private Field findField(String name, Collection<Field> fields) {
    return (Field) find(fields, f -> name.equals(((Field) f).getName()));
  }

  private void assertType(MetadataType type, Class<?> rawType) {
    assertThat(rawType.isAssignableFrom(JavaTypeUtils.getType(type)), is(true));
  }

  private Method getMethod(String methodName, Class<?>... parameterTypes) throws Exception {
    return getClass().getMethod(methodName, parameterTypes);
  }

  private void assertDictionary(MetadataType metadataType, Class<?> valueType) {
    assertThat(metadataType, is(instanceOf(ObjectType.class)));
    ObjectType dictionaryType = (ObjectType) metadataType;
    assertType(dictionaryType, Map.class);

    assertThat(dictionaryType.getOpenRestriction().isPresent(), is(true));
    assertType(dictionaryType.getOpenRestriction().get(), valueType);
  }

  private void assertList(MetadataType metadataType, Class<?> listItemType) {
    assertThat(metadataType, is(instanceOf(ArrayType.class)));
    assertType(metadataType, List.class);
    MetadataType itemMetadataType = ((ArrayType) metadataType).getType();
    assertType(itemMetadataType, listItemType);
  }

  public Map<String, Apple> foo() {
    return new HashMap<>();
  }

  public Result<String, Object> operationResult() {
    return null;
  }

  public PagingProvider<Object, String> pagingProvider() {
    return null;
  }

  public PagingProvider<Object, Result<String, Object>> pagingProviderOperationResult() {
    return null;
  }

  public int bar(String s, Long l, Apple apple, Map<Banana, Kiwi> fruits) {
    return Objects.hash(s, l, apple, fruits);
  }


  public List<FruitBasket> getBaskets() {
    return baskets;
  }

  public void setBaskets(List<FruitBasket> baskets) {
    this.baskets = baskets;
  }

  private void assertAttributesType(String method) throws Exception {
    MetadataType attributesType = IntrospectionUtils.getMethodReturnAttributesType(getMethod(method), TYPE_LOADER);
    assertThat(attributesType, is(instanceOf(ObjectType.class)));
    assertType(attributesType, Object.class);
  }

  private void assertVoidAttributesType(String method) throws Exception {
    MetadataType attributesType = IntrospectionUtils.getMethodReturnAttributesType(getMethod(method), TYPE_LOADER);
    assertThat(attributesType, is(instanceOf(VoidType.class)));

  }

  private void assertReturnType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method), TYPE_LOADER);
    assertThat(returnType, is(instanceOf(StringType.class)));
    assertType(returnType, String.class);
  }

  private void assertPagingProviderReturnType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method), TYPE_LOADER);

    assertThat(returnType, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) returnType).getType(), is(instanceOf(StringType.class)));
    assertType(((ArrayType) returnType).getType(), String.class);
  }

  private void assertPagingProviderReturnResultType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method), TYPE_LOADER);

    assertThat(returnType, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) returnType).getType(), is(instanceOf(StringType.class)), is(instanceOf(ObjectType.class)));
  }

  private class TestPagingProvider implements PagingProvider<Object, Banana> {

    @Override
    public List<Banana> getPage(Object connection) {
      return null;
    }

    @Override
    public Optional<Integer> getTotalResults(Object connection) {
      return null;
    }

    @Override
    public void close(Object connection) throws MuleException {}
  }
}
