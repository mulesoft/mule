/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.module.json.TestBean;
import org.mule.module.json.util.JsonUtils;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.ezmorph.bean.MorphDynaBean;

public class JsonDynaBeanCollectionsTestCase extends AbstractMuleTestCase
{

    public void testTransformBean() throws Exception
    {


        TestBean testBean = new TestBean("json", 23, 2.2, "function(i){ return i; }");
        ObjectToJson transformer = createObject(ObjectToJson.class);
        String transformed = (String) transformer.transform(testBean);

        TestBean testBean2 = (TestBean) JsonUtils.getObjectFromJsonString(transformed, transformer.getJsonConfig(), TestBean.class);
        assertEquals("json", testBean2.getName());
        assertEquals(23, testBean2.getId());
        assertEquals(2.2, testBean2.getDoublev());
        assertEquals("function(i){ return i; }", testBean2.getFunc1());
    }

    public void testTransformStringArray() throws Exception
    {
        String[] list = new String[]{"foo", "bar"};
        ObjectToJson trans1 = createObject(ObjectToJson.class);
        String transformed = (String) trans1.transform(list);

        JsonToObject trans2 = new JsonToObject();
        trans2.setReturnClass(String[].class);
        trans2.initialise();

        Object[] list2 = (Object[]) JsonUtils.getObjectFromJsonString(transformed, trans2.getJsonConfig(), trans2.getReturnClass());

        assertNotNull(list2);
        assertTrue(list2.getClass().isArray());
        assertEquals("foo", list2[0]);
        assertEquals("bar", list2[1]);
    }

    public void testTransformObjectArray() throws Exception
    {
        Orange[] oranges = new Orange[]{new Orange(6, 2.3, "smallish"), new Orange(10, 6.2, "massive")};
        ObjectToJson trans1 = createObject(ObjectToJson.class);
        String transformed = (String) trans1.transform(oranges);

        JsonToObject trans2 = new JsonToObject();
        trans2.setReturnClass(Orange[].class);
        initialiseObject(trans2);

        Orange[] oranges2 = (Orange[]) JsonUtils.getObjectFromJsonString(transformed, trans2.getJsonConfig(), trans2.getReturnClass());

        assertNotNull(oranges2);
        assertEquals(2, oranges2.length);
        assertEquals(new Integer(6), oranges2[0].getSegments());
        assertEquals(new Double(2.3), oranges2[0].getRadius());
        assertEquals("smallish", oranges2[0].getBrand());

        assertEquals(new Integer(10), oranges2[1].getSegments());
        assertEquals(new Double(6.2), oranges2[1].getRadius());
        assertEquals("massive", oranges2[1].getBrand());
    }

    public void testTransformList() throws Exception
    {
        ArrayList list = new ArrayList();
        Apple apple = new Apple();
        apple.setWashed(true);
        Orange orange = new Orange(7, new Double(3.7), "nice!");
        list.add(apple);
        list.add(orange);

        ObjectToJson trans1 = createObject(ObjectToJson.class);
        String transformed = (String) trans1.transform(list);

        JsonToObject trans2 = new JsonToObject();
        trans2.setReturnClass(ArrayList.class);
        initialiseObject(trans2);
        List list2 = (List) JsonUtils.getObjectFromJsonString(transformed, trans2.getJsonConfig(), trans2.getReturnClass());

        assertNotNull(list2);
        assertTrue(list2.get(0) instanceof MorphDynaBean);
        Apple appleback = (Apple) list.get(0);
        assertTrue(appleback.isWashed());
        assertTrue(list2.get(1) instanceof MorphDynaBean);
        Orange orangeback = (Orange) list.get(1);
        assertEquals(new Double(3.7), orangeback.getRadius());
        assertEquals(new Integer(7), orangeback.getSegments());
        assertEquals("nice!", orangeback.getBrand());
    }

    public void testTransformSet() throws Exception
    {
        Set set = new HashSet();
        set.add("foo");
        set.add("bar");
        ObjectToJson trans1 = createObject(ObjectToJson.class);
        String transformed = (String) trans1.transform(set);

        JsonToObject trans2 = new JsonToObject();
        trans2.setReturnClass(HashSet.class);
        initialiseObject(trans2);
        Set set2 = (Set) JsonUtils.getObjectFromJsonString(transformed, trans2.getJsonConfig(), trans2.getReturnClass());

        assertNotNull(set);

        Iterator i = set2.iterator();
        assertEquals("foo", i.next());
        assertEquals("bar", i.next());
    }

}
