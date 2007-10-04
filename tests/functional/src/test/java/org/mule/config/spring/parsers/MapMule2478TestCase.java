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

import java.util.List;
import java.util.Map;

public class MapMule2478TestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/map-mule-2478-test.xml";
    }

    public void testMap()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
//        ChildBean child1 = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
//        assertEquals("string1", child1.getString());
//        assertNotNull(child1.getList());
//        assertEquals("list1", child1.getList().get(0));
        Map map = orphan.getMap();
        assertNotNull(map);
        assertTrue(map.containsKey("string"));
        assertEquals("string2", map.get("string"));
        assertTrue(map.containsKey("name"));
        assertEquals("child2", map.get("name"));
        assertTrue(map.containsKey("list"));
        assertEquals("list2", ((List) map.get("list")).get(0));
    }

}
