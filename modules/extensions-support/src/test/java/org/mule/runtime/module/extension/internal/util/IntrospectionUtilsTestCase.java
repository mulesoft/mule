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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getPagingProviderTypes;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.dictionaryOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.builder.StringTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBox;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.test.petstore.extension.PhoneNumber;
import org.mule.test.petstore.extension.TransactionalPetStoreClient;
import org.mule.test.petstore.extension.TransactionalPetStoreConnectionProvider;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.processing.ProcessingEnvironment;

import com.google.testing.compile.CompilationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.core.ResolvableType;

@SmallTest
@RunWith(Parameterized.class)
public class IntrospectionUtilsTestCase extends AbstractMuleTestCase {

  private static final String CLASS = "CLASS";
  public static final String OPERATION_RESULT = "operationResult";
  public static final String PAGING_PROVIDER = "pagingProvider";
  public static final String PAGING_PROVIDER_OPERATION_RESULT = "pagingProviderOperationResult";
  public static final String FOO = "foo";
  public static final String LIST_RESULT_STRING = "listResultStringObject";
  public static ProcessingEnvironment processingEnvironment;
  private List<FruitBasket> baskets;


  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Rule
  public CompilationRule compilationRule = new CompilationRule();

  private ReflectionCache reflectionCache = new ReflectionCache();

  @Parameterized.Parameter
  public String mode;

  @Parameterized.Parameter(1)
  public BiFunction<String, Class[], OperationElement> operationSupplier;

  @Parameterized.Parameter(2)
  public Function<Class, Type> typeSupplier;

  @Parameterized.Parameter(3)
  public Function<Class, SourceElement> sourceSupplier;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    List<Object[]> objects = new ArrayList<>();

    Function<Class, Type> javaTypeSupplier =
        aClass -> new TypeWrapper(aClass, new DefaultExtensionsTypeLoaderFactory().createTypeLoader());

    objects.add(new Object[] {CLASS, new ClassOperationSupplier(), javaTypeSupplier, new ClassSourceSupplier()});
    return objects;
  }

  @Before
  public void setUp() {
    processingEnvironment = mock(ProcessingEnvironment.class);
    when(processingEnvironment.getTypeUtils()).thenReturn(compilationRule.getTypes());
    when(processingEnvironment.getElementUtils()).thenReturn(compilationRule.getElements());
  }

  @Test
  public void getMethodReturnType() throws Exception {
    MetadataType metadataType = IntrospectionUtils.getMethodReturnType(getMethod(FOO));
    assertDictionary(metadataType, Apple.class);
  }

  @Test
  public void getObjectMethodReturnType() throws Exception {
    MetadataType metadataType = IntrospectionUtils.getMethodReturnType(getMethod("methodReturnObject"));

    assertThat(metadataType, is(instanceOf(AnyType.class)));
  }

  @Test
  public void getGenericsFromReturnType() throws Exception {
    OperationElement listResultStringObject = getMethod(LIST_RESULT_STRING, null);
    Type returnType = listResultStringObject.getReturnType();
    List<Type> listGeneric = returnType.getSuperTypeGenerics(Collection.class);
    assertThat(listGeneric.get(0).getTypeName(), containsString("Result"));
    List<Type> resultGenerics = listGeneric.get(0).getSuperTypeGenerics(Result.class);
    assertThat(resultGenerics.get(0).getTypeName(), containsString("String"));
  }

  @Test
  public void getGenericsFromIndirectInterface() throws Exception {
    Type classType = typeSupplier.apply(OtherClass.class);
    List<Type> listGeneric = classType.getSuperTypeGenerics(RootInterface.class);
    assertThat(listGeneric.get(0).getTypeName(), containsString("Integer"));
    assertThat(listGeneric.get(1).getTypeName(), containsString("String"));
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
  public void getNullMethodReturnType() {
    IntrospectionUtils.getMethodReturnType(null);
  }

  @Test
  public void getArgumentlessMethodArgumentTypes() throws Exception {
    assumeThat(mode, is(CLASS));
    MetadataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod(FOO).getMethod().get(), TYPE_LOADER);
    assertNotNull(types);
    assertEquals(0, types.length);
  }

  @Test
  public void getMethodArgumentTypes() throws Exception {
    assumeThat(mode, is(CLASS));
    MetadataType[] types = IntrospectionUtils
        .getMethodArgumentTypes(getMethod("bar", String.class, Long.class, Apple.class, Map.class).getMethod().get(),
                                TYPE_LOADER);
    assertNotNull(types);
    assertEquals(4, types.length);

    assertType(types[0], String.class);
    assertType(types[1], Long.class);
    assertType(types[2], Apple.class);
    assertDictionary(types[3], Kiwi.class);
  }

  @Test
  public void getInterfaceGenerics() {
    Type connectionProvider = typeSupplier.apply(TransactionalPetStoreConnectionProvider.class);
    List<Type> interfaceGenerics = connectionProvider.getSuperTypeGenerics(ConnectionProvider.class);

    assertThat(interfaceGenerics.size(), is(1));
    Type type = interfaceGenerics.get(0);
    assertThat(type.isSameType(TransactionalPetStoreClient.class), is(true));
  }

  @Test
  public void resultWithNoGenericsIsAnyType() throws Exception {
    MetadataType resultWithNoGenerics = getMethod("resultWithNoGenerics").getReturnMetadataType();
    MetadataType resultWithWildcardGenerics = getMethod("resultWithWildcardGenerics").getReturnMetadataType();

    assertThat(resultWithNoGenerics, is(instanceOf(AnyType.class)));
    assertThat(resultWithWildcardGenerics, is(instanceOf(AnyType.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNullMethodArgumentTypes() throws Exception {
    IntrospectionUtils.getMethodArgumentTypes(null, TYPE_LOADER);
  }

  @Test
  public void getFieldDataType() throws Exception {
    MetadataType type = getFieldMetadataType(IntrospectionUtilsTestCase.class.getDeclaredField("baskets"), TYPE_LOADER);
    assertList(type, FruitBasket.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNullFieldDataType() throws Exception {
    getFieldMetadataType(null, TYPE_LOADER);
  }

  @Test
  public void getEmptyExposedPojoFields() {
    Collection<Field> exposedFields = getExposedFields(FruitBasket.class, reflectionCache);
    assertThat(exposedFields, is(empty()));
  }

  @Test
  public void getFieldsWithGettersOnly() {
    Collection<Field> fieldsWithGetters = getFieldsWithGetters(PhoneNumber.class, reflectionCache);
    assertThat(fieldsWithGetters.size(), is(4));
  }

  @Test
  public void getRawListReturnTypeNoGenerics() {
    Collection<Field> fieldsWithGetters = getFieldsWithGetters(PhoneNumber.class, reflectionCache);
    assertThat(fieldsWithGetters.size(), is(4));
  }

  @Test
  public void getWildCardFieldsDataTypes() {

    Collection<Field> exposedFields = getFieldsWithGetters(FruitBox.class, reflectionCache);
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
    Pair<Type, Type> pagingProviderTypes = getPagingProviderTypes(new TypeWrapper(pagingProvider, TYPE_LOADER));

    assertThat(pagingProviderTypes.getFirst().getDeclaringClass().get(), equalTo(Object.class));
    assertThat(pagingProviderTypes.getSecond().getDeclaringClass().get(), equalTo(Result.class));
  }

  @Test
  public void getPagingProviderImplementationTypesReturnType() throws Exception {
    OperationElement getPagingProvider = getMethod("getPagingProvider");
    MetadataType methodReturnType = IntrospectionUtils.getMethodReturnType(getPagingProvider);
    assertThat(methodReturnType, instanceOf(ArrayType.class));

    MetadataType valueType = ((ArrayType) methodReturnType).getType();
    assertThat(valueType, instanceOf(MessageMetadataType.class));

    MessageMetadataType messageType = (MessageMetadataType) valueType;
    MetadataType payloadType = messageType.getPayloadType().get();
    MetadataType attributesTypes = messageType.getAttributesType().get();

    //These assertions are too simple, but AST Loader doesn't enrich with the same annotations as the Java does
    //making impossible to do an equals assertion.
    assertThat(payloadType.getAnnotation(TypeIdAnnotation.class).get().getValue(), is(Banana.class.getName()));
    assertThat(attributesTypes.getAnnotation(TypeIdAnnotation.class).get().getValue(), is(Apple.class.getName()));
  }

  @Test
  public void getProperties() {
    Set<Field> fields = IntrospectionUtils.getFieldsWithGetters(SomePojo.class, reflectionCache);
    Type somePojo = typeSupplier.apply(SomePojo.class);

    List<FieldElement> fieldsWithGetters = IntrospectionUtils.getFieldsWithGetters(somePojo);
    assertThat(fieldsWithGetters.size(), is(fields.size()));
  }

  @Test
  public void listWithNoGenerics() throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod("listNoGenerics"));
    assertThat(returnType, instanceOf(ArrayType.class));
    assertThat(((ArrayType) returnType).getType(), instanceOf(ObjectType.class));
  }

  @Test
  public void mapWithNoGenerics() throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod("mapNoGenerics"));
    assertThat(returnType, instanceOf(ObjectType.class));
    assertThat(((ObjectType) returnType).getOpenRestriction().get(), instanceOf(AnyType.class));
  }

  @Test
  public void getSourceGenerics() {
    Type type = typeSupplier.apply(ThirdLevelSource.class);
    List<Type> superTypeGenerics = type.getSuperTypeGenerics(Source.class);

    assertThat(superTypeGenerics.size(), is(2));
    Type stringListType = superTypeGenerics.get(0);
    assertThat(stringListType.isSameType(List.class), is(true));
    assertThat(stringListType.getGenerics().get(0).getConcreteType().isSameType(String.class), is(true));
    assertThat(superTypeGenerics.get(1).isSameType(Integer.class), is(true));
  }

  @Test
  public void testGetMethodFromType() {
    Type pojoWithEqualsAndHashCode = typeSupplier.apply(PojoWithEqualsAndHashCode.class);
    Optional<MethodElement> equals = pojoWithEqualsAndHashCode.getMethod("equals", Object.class);
    Optional<MethodElement> equalsWithoutParams = pojoWithEqualsAndHashCode.getMethod("equals");
    Optional<MethodElement> hashCode = pojoWithEqualsAndHashCode.getMethod("hashCode");
    Optional<MethodElement> otherMethod = pojoWithEqualsAndHashCode.getMethod("otherMethod");
    Optional<MethodElement> parametersWithoutOrder =
        pojoWithEqualsAndHashCode.getMethod("someMethod", String.class, Object.class);

    assertThat(equals.isPresent(), is(true));
    assertThat(equalsWithoutParams.isPresent(), is(false));
    assertThat(hashCode.isPresent(), is(true));
    assertThat(otherMethod.isPresent(), is(false));
    assertThat(parametersWithoutOrder.isPresent(), is(false));
  }

  @Test
  public void testGetMethodFromTypeWithInheritance() {
    Type extendsPojoWithEqualsAndHashCode = typeSupplier.apply(ExtendsPojoWithEqualsAndHashCode.class);
    Optional<MethodElement> equals = extendsPojoWithEqualsAndHashCode.getMethod("equals", Object.class);
    Optional<MethodElement> hashCode = extendsPojoWithEqualsAndHashCode.getMethod("hashCode");
    Optional<MethodElement> otherMethod = extendsPojoWithEqualsAndHashCode.getMethod("otherMethod");

    assertThat(equals.isPresent(), is(true));
    assertThat(hashCode.isPresent(), is(true));
    assertThat(otherMethod.isPresent(), is(false));
  }

  @Test
  public void testGetMethodFromTypeDefinedInObject() {
    Type type = typeSupplier.apply(SomePojo.class);
    Optional<MethodElement> equals = type.getMethod("equals", Object.class);

    assertThat(equals.isPresent(), is(false));
  }

  @Test
  public void getSourceMetadataType() {
    SourceElement source = sourceSupplier.apply(ThirdLevelSource.class);
    MetadataType returnType = source.getReturnMetadataType();

    assertThat(returnType, is(instanceOf(ArrayType.class)));
    ArrayType arrayType = (ArrayType) returnType;

    MetadataType itemType = arrayType.getType();
    assertThat(itemType, is(instanceOf(StringType.class)));
  }


  @Test
  public void getByteArrayOutputType() throws Exception {
    OperationElement operation = getMethod("byteArray");
    MetadataType metadataType = operation.getOperationReturnMetadataType();
    assertThat(metadataType, instanceOf(AnyType.class));
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

  private OperationElement getMethod(String methodName, Class<?>... parameterTypes) throws Exception {
    return operationSupplier.apply(methodName, parameterTypes);
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

  public Object methodReturnObject() {
    return this;
  }

  public List<Result<String, Object>> listResultStringObject() {
    return null;
  }

  public List listNoGenerics() {
    return new ArrayList();
  }

  public Map mapNoGenerics() {
    return new LinkedHashMap();
  }

  public Result<String, Object> operationResult() {
    return null;
  }

  public Result resultWithNoGenerics() {
    return null;
  }

  public Result<?, ?> resultWithWildcardGenerics() {
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

  public TestPagingProvider getPagingProvider() {
    return null;
  }

  public List<FruitBasket> getBaskets() {
    return baskets;
  }

  public void setBaskets(List<FruitBasket> baskets) {
    this.baskets = baskets;
  }

  public Byte[] byteArray() {
    return null;
  }

  private void assertAttributesType(String method) throws Exception {
    MetadataType attributesType = IntrospectionUtils.getMethodReturnAttributesType(getMethod(method));
    assertThat(attributesType, is(instanceOf(ObjectType.class)));
    assertType(attributesType, Object.class);
  }

  private void assertVoidAttributesType(String method) throws Exception {
    MetadataType attributesType = IntrospectionUtils.getMethodReturnAttributesType(getMethod(method));
    assertThat(attributesType, is(instanceOf(VoidType.class)));

  }

  private void assertReturnType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method));
    assertThat(returnType, is(instanceOf(StringType.class)));
    assertType(returnType, String.class);
  }

  private void assertPagingProviderReturnType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method));

    assertThat(returnType, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) returnType).getType(), is(instanceOf(StringType.class)));
    assertType(((ArrayType) returnType).getType(), String.class);
  }

  private void assertPagingProviderReturnResultType(String method) throws Exception {
    MetadataType returnType = IntrospectionUtils.getMethodReturnType(getMethod(method));

    assertThat(returnType, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) returnType).getType(), is(instanceOf(StringType.class)), is(instanceOf(ObjectType.class)));
  }

  public static class ThirdLevelSource extends SecondLevelSource<Integer> {
  }

  public static class SecondLevelSource<T extends Serializable> extends FirstLevelSource<List<String>, T> {
  }

  public static class FirstLevelSource<P, A> extends Source<P, A> {

    @Override
    public void onStart(SourceCallback<P, A> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  public static class SimpleSource extends Source<List<String>, Integer> {

    @Override
    public void onStart(SourceCallback<List<String>, Integer> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  public interface RootInterface<T, A> {

    public void doSomething();
  }

  public interface SecondInterface<K> extends RootInterface<Integer, K> {

    public void doSomethingElse();
  }

  public interface ThirdInterface extends SecondInterface<String> {

    public void doNothing();
  }

  public static class BaseClass implements ThirdInterface {

    @Override
    public void doSomething() {}

    @Override
    public void doSomethingElse() {}

    @Override
    public void doNothing() {}
  }

  public static class OtherClass extends BaseClass {
  }

  public static class SomePojo {

    private String aString;
    private Integer someNumber;
    private String nonProperty;

    public String getaString() {
      return aString;
    }

    public void setaString(String aString) {
      this.aString = aString;
    }

    public Integer getSomeNumber() {
      return someNumber;
    }

    public void setSomeNumber(Integer someNumber) {
      this.someNumber = someNumber;
    }
  }

  public static class PojoWithEqualsAndHashCode {

    private String name;

    public boolean someMethod(Object object, String string) {
      return true;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      PojoWithEqualsAndHashCode that = (PojoWithEqualsAndHashCode) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class ExtendsPojoWithEqualsAndHashCode extends PojoWithEqualsAndHashCode {

  }

  private class TestPagingProvider implements PagingProvider<Object, Result<Banana, Apple>> {

    @Override
    public List<Result<Banana, Apple>> getPage(Object connection) {
      return null;
    }

    @Override
    public Optional<Integer> getTotalResults(Object connection) {
      return null;
    }

    @Override
    public void close(Object connection) throws MuleException {}
  }

  private static class ClassSourceSupplier implements Function<Class<?>, SourceElement> {

    @Override
    public SourceElement apply(Class className) {
      return new SourceTypeWrapper(className, TYPE_LOADER);
    }
  }

  private static class ClassOperationSupplier implements BiFunction<String, Class[], OperationElement> {

    @Override
    public OperationElement apply(String methodName, Class[] paramTypes) {
      try {
        return new OperationWrapper(IntrospectionUtilsTestCase.class.getMethod(methodName, paramTypes), TYPE_LOADER);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException();
      }
    }
  }
}
