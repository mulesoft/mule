/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.generics;

import org.mule.tck.AbstractMuleTestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericsUtilsTestCase extends AbstractMuleTestCase
{

    protected Class<?> targetClass;

    protected String methods[];

    protected Type expectedResults[];

    @Override
    protected void doSetUp() throws Exception
    {
        this.targetClass = Foo.class;
        this.methods = new String[]{"a", "b", "b2", "b3", "c", "d", "d2", "d3", "e", "e2", "e3"};
        this.expectedResults = new Class[]{
                Integer.class, null, Set.class, Set.class, null, Integer.class,
                Integer.class, Integer.class, Integer.class, Integer.class, Integer.class};
    }

    protected Type getType(Method method)
    {
        return GenericsUtils.getMapValueReturnType(method);
    }

    public void testA() throws Exception
    {
        executeTest();
    }

    public void testB() throws Exception
    {
        executeTest();
    }

    public void testB2() throws Exception
    {
        executeTest();
    }

    public void testB3() throws Exception
    {
        executeTest();
    }

    public void testC() throws Exception
    {
        executeTest();
    }

    public void testD() throws Exception
    {
        executeTest();
    }

    public void testD2() throws Exception
    {
        executeTest();
    }

    public void testD3() throws Exception
    {
        executeTest();
    }

    public void testE() throws Exception
    {
        executeTest();
    }

    public void testE2() throws Exception
    {
        executeTest();
    }

    public void testE3() throws Exception
    {
        executeTest();
    }

    public void testProgrammaticListIntrospection() throws Exception
    {
        Method setter = GenericBean.class.getMethod("setResourceList", List.class);
        assertEquals(String.class,
                GenericsUtils.getCollectionParameterType(new MethodParameter(setter, 0)));

        Method getter = GenericBean.class.getMethod("getResourceList");
        assertEquals(String.class,
                GenericsUtils.getCollectionReturnType(getter));
    }


    private abstract class CustomMap<T> extends AbstractMap<String, Integer>
    {
    }


    private abstract class OtherCustomMap<T> implements Map<String, Integer>
    {
    }


    private interface Foo
    {

        Map<String, Integer> a();

        Map<?, ?> b();

        Map<?, ? extends Set> b2();

        Map<?, ? super Set> b3();

        Map c();

        CustomMap<Date> d();

        CustomMap<?> d2();

        CustomMap d3();

        OtherCustomMap<Date> e();

        OtherCustomMap<?> e2();

        OtherCustomMap e3();
    }


    protected void executeTest() throws NoSuchMethodException
    {
        String methodName = getName().trim().replaceFirst("test", "").toLowerCase();
        for (int i = 0; i < this.methods.length; i++)
        {
            if (methodName.equals(this.methods[i]))
            {
                Method method = this.targetClass.getMethod(methodName);
                Type type = getType(method);
                assertEquals(this.expectedResults[i], type);
                return;
            }
        }
        throw new IllegalStateException("Bad test data");
    }


}
