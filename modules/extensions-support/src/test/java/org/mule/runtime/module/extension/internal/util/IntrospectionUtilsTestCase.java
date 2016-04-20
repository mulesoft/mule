/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.dictionaryOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBox;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.test.heisenberg.extension.model.LifetimeInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

@SmallTest
public class IntrospectionUtilsTestCase extends AbstractMuleTestCase
{

    private List<FruitBasket> baskets;

    @Test
    public void getMethodReturnType() throws Exception
    {
        MetadataType metadataType = IntrospectionUtils.getMethodReturnType(getMethod("foo"), TYPE_LOADER);
        assertDictionary(metadataType, String.class, Apple.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullMethodReturnType() throws Exception
    {
        IntrospectionUtils.getMethodReturnType(null, TYPE_LOADER);
    }

    @Test
    public void getArgumentlessMethodArgumentTypes() throws Exception
    {
        MetadataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod("foo"), TYPE_LOADER);
        assertNotNull(types);
        assertEquals(0, types.length);
    }

    @Test
    public void getMethodArgumentTypes() throws Exception
    {
        MetadataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod("bar", String.class, Long.class, Apple.class, Map.class), TYPE_LOADER);
        assertNotNull(types);
        assertEquals(4, types.length);

        assertType(types[0], String.class);
        assertType(types[1], Long.class);
        assertType(types[2], Apple.class);
        assertDictionary(types[3], Banana.class, Kiwi.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullMethodArgumentTypes() throws Exception
    {
        IntrospectionUtils.getMethodArgumentTypes(null, TYPE_LOADER);
    }

    @Test
    public void getFieldDataType() throws Exception
    {
        MetadataType type = IntrospectionUtils.getFieldMetadataType(getClass().getDeclaredField("baskets"), TYPE_LOADER);
        assertList(type, FruitBasket.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullFieldDataType() throws Exception
    {
        IntrospectionUtils.getFieldMetadataType(null, TYPE_LOADER);
    }

    @Test
    public void getNoAnnotatedExposedPojoFields()
    {
        Collection<Field> exposedFields = IntrospectionUtils.getExposedFields(LifetimeInfo.class);
        assertThat(exposedFields, is(not(empty())));
        assertThat(exposedFields.size(), is(2));
        assertField("dateOfBirth", TYPE_LOADER.load(Date.class), exposedFields);
        assertField("dateOfDeath", TYPE_LOADER.load(Calendar.class), exposedFields);
    }

    @Test
    public void getEmptyExposedPojoFields()
    {
        Collection<Field> exposedFields = IntrospectionUtils.getExposedFields(FruitBasket.class);
        assertThat(exposedFields, is(empty()));
    }

    @Test
    public void getWildCardFieldsDataTypes()
    {

        Collection<Field> exposedFields = IntrospectionUtils.getExposedFields(FruitBox.class);
        assertNotNull(exposedFields);
        assertEquals(6, exposedFields.size());
        assertField("fruitLikeList", arrayOf(List.class, objectTypeBuilder(Fruit.class)), exposedFields);
        assertField("wildCardList", arrayOf(List.class, objectTypeBuilder(Object.class)), exposedFields);
        assertField("rawList", arrayOf(List.class, objectTypeBuilder(Object.class)), exposedFields);
        assertField("wildCardMap", dictionaryOf(Map.class, objectTypeBuilder(Object.class), objectTypeBuilder(Object.class)), exposedFields);
        assertField("rawMap", dictionaryOf(Map.class, objectTypeBuilder(Object.class), objectTypeBuilder(Object.class)), exposedFields);
        assertField("fruitLikeMap", dictionaryOf(Map.class, objectTypeBuilder(Object.class), objectTypeBuilder(Fruit.class)), exposedFields);
    }

    private void assertField(String name, MetadataType metadataType, Collection<Field> fields)
    {
        Field field = findField(name, fields);
        assertThat(field, is(notNullValue()));
        assertThat(field.getName(), equalTo(name));
        assertThat(field.getType(), equalTo(JavaTypeUtils.getType(metadataType)));
    }

    private Field findField(String name, Collection<Field> fields)
    {
        return (Field) CollectionUtils.find(fields, f -> name.equals(((Field) f).getName()));
    }

    private void assertType(MetadataType type, Class<?> rawType)
    {
        assertThat(rawType.isAssignableFrom(JavaTypeUtils.getType(type)), is(true));
    }

    private Method getMethod(String methodName, Class<?>... parameterTypes) throws Exception
    {
        return getClass().getMethod(methodName, parameterTypes);
    }

    private void assertDictionary(MetadataType metadataType, Class<?> keyType, Class<?> valueType)
    {
        assertThat(metadataType, is(instanceOf(DictionaryType.class)));
        DictionaryType dictionaryType = (DictionaryType) metadataType;
        assertType(dictionaryType, Map.class);

        assertType(dictionaryType.getKeyType(), keyType);
        assertType(dictionaryType.getValueType(), valueType);
    }

    private void assertList(MetadataType metadataType, Class<?> listItemType)
    {
        assertThat(metadataType, is(instanceOf(ArrayType.class)));
        assertType(metadataType, List.class);
        MetadataType itemMetadataType = ((ArrayType) metadataType).getType();
        assertType(itemMetadataType, listItemType);
    }

    public Map<String, Apple> foo()
    {
        return new HashMap<>();
    }

    public int bar(String s, Long l, Apple apple, Map<Banana, Kiwi> fruits)
    {
        return Objects.hash(s, l, apple, fruits);
    }

    public List<FruitBasket> getBaskets()
    {
        return baskets;
    }

    public void setBaskets(List<FruitBasket> baskets)
    {
        this.baskets = baskets;
    }
}
