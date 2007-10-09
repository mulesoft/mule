/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;
import org.mule.util.object.ObjectFactory;

import java.util.List;
import java.util.Map;

public class MapMule2478TestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/map-mule-2478-test.xml";
    }

    public void testDirectChild()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        ChildBean child1 = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
        assertEquals("string1", child1.getString());
        assertNotNull(child1.getList());
        assertEquals("list1", child1.getList().get(0));
    }

    public void testMappedChild()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        Map map = orphan.getMap();
        assertNotNull(map);
        assertTrue(map.containsKey("string"));
        assertEquals("string2", map.get("string"));
        assertTrue(map.containsKey("name"));
        assertEquals("child2", map.get("name"));
        assertTrue(map.containsKey("list"));
        assertEquals("list2", ((List) map.get("list")).get(0));
    }

    public void testFactory() throws Exception
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        ObjectFactory factory = (ObjectFactory) orphan.getObject();
        assertNotNull(factory);
        Object product = factory.getOrCreate();
        assertNotNull(product);
        assertTrue(product instanceof ChildBean);
        ChildBean child3 = (ChildBean) product;
        assertEquals("string3", child3.getString());
        assertNotNull(child3.getList());
        assertEquals("list3", child3.getList().get(0));
    }

}
