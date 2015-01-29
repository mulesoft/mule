/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.AbstractBean;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ClassUtils;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNamespaceTestCase extends FunctionalTestCase
{

    @Test
    public void testParse()
    {
        // just parse the config
    }

    protected Object assertBeanExists(String name, Class clazz)
    {
        Object bean = muleContext.getRegistry().lookupObject(name);
        assertNotNull(name + " bean missing", bean);
        assertTrue(bean.getClass().equals(clazz));
        logger.debug("found bean " + name + "/" + ClassUtils.getSimpleName(bean.getClass()));
        return bean;
    }

    protected Object assertContentExists(Object object, Class clazz)
    {
        assertNotNull(ClassUtils.getSimpleName(clazz) + " content missing", object);
        assertTrue(clazz.isAssignableFrom(object.getClass()));
        logger.debug("found content " + ClassUtils.getSimpleName(object.getClass()));
        return object;
    }

    protected void assertBeanPopulated(AbstractBean bean, String name)
    {
        assertMapExists(bean.getMap(), name);
        assertListExists(bean.getList(), name);
        String string = bean.getString();
        assertNotNull("string for " + name, string);
        assertEquals(name + "String", string);
    }

    protected void assertMapExists(Map map, String name)
    {
        assertNotNull("map for " + name, map);
        assertMapEntryExists(map, name, 1);
        assertMapEntryExists(map, name, 2);
    }

    protected void assertMapEntryExists(Map map, String name, int index)
    {
        String key = "key" + index;
        Object value = map.get(key);
        assertNotNull(key + " returns null", value);
        assertTrue(value instanceof String);
        assertEquals(name + "Map" + index, value);
    }

    protected void assertListExists(List list, String name)
    {
        assertNotNull("list for " + name, list);
        assertListEntryExists(list, name, 1);
        assertListEntryExists(list, name, 2);
    }

    protected void assertListEntryExists(List list, String name, int index)
    {
        String value = name + "List" + index;
        assertTrue(value, list.contains(value));
    }

}
