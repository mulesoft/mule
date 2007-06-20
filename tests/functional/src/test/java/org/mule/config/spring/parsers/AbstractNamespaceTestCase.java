/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.tck.FunctionalTestCase;
import org.mule.util.ClassUtils;

import java.util.List;
import java.util.Map;

public abstract class AbstractNamespaceTestCase extends FunctionalTestCase
{

    private String config;

    public AbstractNamespaceTestCase(String config)
    {
        this.config = config;
    }

    protected String getConfigResources()
    {
        return config;
    }

    public void testParse()
    {
        // just parse the config
    }

    protected Object beanExists(String name, Class clazz)
    {
        Object bean = managementContext.getRegistry().lookupObject(name, clazz);
        assertNotNull(name + " bean missing", bean);
        assertTrue(bean.getClass().equals(clazz));
        logger.debug("found bean " + name + "/" + ClassUtils.getSimpleName(bean.getClass()));
        return bean;
    }

    protected Object contentExists(Object object, Class clazz)
    {
        assertNotNull(ClassUtils.getSimpleName(clazz) + " content missing", object);
        assertTrue(clazz.isAssignableFrom(object.getClass()));
        logger.debug("found content " + ClassUtils.getSimpleName(object.getClass()));
        return object;
    }

    protected void populated(AbstractBean bean, String name)
    {
        mapExists(bean.getMap(), name);
        listExists(bean.getList(), name);
        String string = bean.getString();
        assertNotNull("string for " + name, string);
        assertEquals(name + "String", string);
    }

    protected void mapExists(Map map, String name)
    {
        assertNotNull("map for " + name, map);
        mapEntryExists(map, name, 1);
        mapEntryExists(map, name, 2);
    }

    protected void mapEntryExists(Map map, String name, int index)
    {
        String key = "key" + index;
        Object value = map.get(key);
        assertNotNull(key + " in " + name, value);
        assertTrue(value instanceof String);
        assertEquals(name + "Map" + index, value);
    }

    protected void listExists(List list, String name)
    {
        assertNotNull("list for " + name, list);
        listEntryExists(list, name, 1);
        listEntryExists(list, name, 2);
    }

    protected void listEntryExists(List list, String name, int index)
    {
        String value = name + "List" + index;
        assertTrue(value, list.contains(value));
    }

}
