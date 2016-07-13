/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.test.config.spring.parsers.beans.ChildBean;
import org.mule.test.config.spring.parsers.beans.OrphanBean;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class MapMule2478TestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/map-mule-2478-test.xml";
    }

    @Test
    public void testDirectChild()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        ChildBean child1 = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
        assertEquals("string1", child1.getString());
        assertNotNull(child1.getList());
        assertEquals("list1", child1.getList().get(0));
    }

    @Test
    public void testMappedChild()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        Map<?, ?> map = orphan.getMap();
        assertNotNull(map);
        assertTrue(map.containsKey("string"));
        assertEquals("string2", map.get("string"));
        assertTrue(map.containsKey("name"));
        assertEquals("child2", map.get("name"));
        assertTrue(map.containsKey("list"));
        assertEquals("list2", ((List<?>) map.get("list")).get(0));
    }

// TODO ComplexComponentDefinitionParser is not longer used, is there any way to rewrite/reuse the "factory" element for testing?
//    @Test
//    public void testFactory() throws Exception
//    {
//        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
//        ObjectFactory factory = (ObjectFactory) orphan.getObject();
//        assertNotNull(factory);
//        Object product = factory.getInstance();
//        assertNotNull(product);
//        assertTrue(product instanceof ChildBean);
//        ChildBean child3 = (ChildBean) product;
//        assertEquals("string3", child3.getString());
//        assertNotNull(child3.getList());
//        assertEquals("list3", child3.getList().get(0));
//    }

}
