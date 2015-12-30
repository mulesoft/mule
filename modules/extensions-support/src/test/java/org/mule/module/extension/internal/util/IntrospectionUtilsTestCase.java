/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import org.mule.extension.api.introspection.DataType;
import org.mule.module.extension.LifetimeInfo;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.util.CollectionUtils;

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
        DataType type = IntrospectionUtils.getMethodReturnType(getMethod("foo"));
        assertEquals(Map.class, type.getRawType());

        DataType[] genericTypes = type.getGenericTypes();
        assertEquals(2, genericTypes.length);
        assertType(genericTypes[0], String.class);
        assertType(genericTypes[1], Apple.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullMethodReturnType() throws Exception
    {
        IntrospectionUtils.getMethodReturnType(null);
    }

    @Test
    public void getArgumentlessMethodArgumentTypes() throws Exception
    {
        DataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod("foo"));
        assertNotNull(types);
        assertEquals(0, types.length);
    }

    @Test
    public void getMethodArgumentTypes() throws Exception
    {
        DataType[] types = IntrospectionUtils.getMethodArgumentTypes(getMethod("bar", String.class, Long.class, Apple.class, Map.class));
        assertNotNull(types);
        assertEquals(4, types.length);

        assertType(types[0], String.class);
        assertType(types[1], Long.class);
        assertType(types[2], Apple.class);
        assertType(types[3], Map.class, Banana.class, Kiwi.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullMethodArgumentTypes() throws Exception
    {
        IntrospectionUtils.getMethodArgumentTypes(null);
    }

    @Test
    public void getFieldDataType() throws Exception
    {
        DataType type = IntrospectionUtils.getFieldDataType(getClass().getDeclaredField("baskets"));
        assertType(type, List.class, FruitBasket.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullFieldDataType() throws Exception
    {
        IntrospectionUtils.getFieldDataType(null);
    }

    @Test
    public void getNoAnnotatedExposedPojoFields()
    {
        Collection<Field> exposedFields = IntrospectionUtils.getExposedFields(LifetimeInfo.class);
        assertThat(exposedFields, is(not(empty())));
        assertThat(exposedFields.size(), is(2));
        assertField("dateOfBirth", DataType.of(Date.class), exposedFields);
        assertField("dateOfDeath", DataType.of(Calendar.class), exposedFields);
    }

    @Test
    public void getEmptyExposedPojoFields()
    {
        Collection<Field> exposedFields = IntrospectionUtils.getExposedFields(FruitBasket.class);
        assertThat(exposedFields, is(empty()));
    }

    private void assertField(String name, DataType dt, Collection<Field> fields)
    {
        Field field = findField(name, fields);
        assertThat(field, is(notNullValue()));
        assertThat(field.getName(), equalTo(name));
        assertThat(field.getType(), equalTo(dt.getRawType()));
    }

    private Field findField(String name, Collection<Field> fields)
    {
        return (Field) CollectionUtils.find(fields, f -> name.equals(((Field) f).getName()));
    }

    private void assertType(DataType type, Class<?> rawType, Class<?>... genericTypes)
    {
        assertEquals(rawType, type.getRawType());
        if (genericTypes != null)
        {
            assertEquals(genericTypes.length, type.getGenericTypes().length);
            for (int i = 0; i < genericTypes.length; i++)
            {
                assertEquals(genericTypes[i], type.getGenericTypes()[i].getRawType());
            }
        }
    }

    private Method getMethod(String methodName, Class<?>... parameterTypes) throws Exception
    {
        return getClass().getMethod(methodName, parameterTypes);
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
