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

import org.mule.config.spring.parsers.beans.OrphanBean;
import org.mule.config.spring.parsers.assembly.MapCombiner;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MapCombinerTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/map-combiner-test.xml";
    }

    public void testProperties()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("checkProps", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        assertMapEntryExists(bean.getMap(), "0", 0);
    }

    public void testCombinedMap()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        for (int i = 0; i < 6; ++i)
        {
            assertMapEntryExists(bean.getMap(), Integer.toString(i+1), i+1);
        }
    }

    public void testReverersedOrder()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        for (int i = 0; i < 2; ++i)
        {
            assertMapEntryExists(bean.getMap(), Integer.toString(i+1), i+1);
        }
    }

    public void testBasicMerge()
    {
        doTestMerge(new MapCombiner(), "[a:[b:B,c:C]]", "[a:[d:D]]", "[a:[b:B,c:C,d:D]]");
    }

    public void testOverwrite()
    {
        MapCombiner combiner = new MapCombiner();
        combiner.setMaxDepth(0);
        doTestMerge(combiner, "[a:[b:B,c:C]]", "[a:[d:D]]", "[a:[d:D]]");
    }

    public void testDeepMerge()
    {
        doTestMerge(new MapCombiner(), "[a:[b:B,c:C,d:[e:E,f:F]]]", "[a:[d:[g:G]]]", "[a:[b:B,c:C,d:[e:E,f:F,g:G]]]");
    }

    public void testRestrictedMerge()
    {
        MapCombiner combiner = new MapCombiner();
        combiner.setMaxDepth(1);
        doTestMerge(combiner, "[a:[b:B,c:C,d:[e:E,f:F]]]", "[a:[d:[g:G]]]", "[a:[b:B,c:C,d:[g:G]]]");
    }

    public void testMergeLists()
    {
        doTestMerge(new MapCombiner(), "[a:(b,c)]", "[a:(d)]", "[a:(b,c,d)]");
    }

    protected void doTestMerge(MapCombiner combiner, String spec1, String spec2, String specResult)
    {
        Map map1 = buildMap(spec1);
        Map map2 = buildMap(spec2);
        Map map3 = buildMap(specResult);
        combiner.setList(new LinkedList());
        combiner.getList().add(map1);
        combiner.getList().add(map2);
        assertFalse(combiner.isEmpty()); // trigger merge
        assertEquals(combiner, map3);
    }

    protected Map buildMap(String spec)
    {
        Map map = new HashMap();
        String empty = fillMap(map, spec);
        assertTrue("after parsing " + spec + " left with " + empty, empty.equals(""));
        return map;
    }

    protected String fillMap(Map map, String spec)
    {
        spec = drop(spec, "[");
        while (! spec.startsWith("]"))
        {
            assertTrue("spec finished early (missing ']'?)", spec.length() > 1);
            String key = spec.substring(0, 1);
            spec = drop(spec, key);
            spec = drop(spec, ":");
            if (spec.startsWith("["))
            {
                Map value = new HashMap();
                spec = fillMap(value, spec);
                map.put(key, value);
            }
            else if (spec.startsWith("("))
            {
                List value = new LinkedList();
                spec = fillList(value, spec);
                map.put(key, value);
            }
            else
            {
                String value = spec.substring(0, 1);
                spec = drop(spec, value);
                map.put(key, value);
            }
            if (spec.startsWith(","))
            {
                spec = drop(spec, ",");
            }
        }
        return drop(spec, "]");
    }

    protected String fillList(List list, String spec)
    {
        spec = drop(spec, "(");
        while (! spec.startsWith(")"))
        {
            assertTrue("spec finished early (missing ')'?)", spec.length() > 1);
            if (spec.startsWith("["))
            {
                Map value = new HashMap();
                spec = fillMap(value, spec);
                list.add(value);
            }
            else if (spec.startsWith("("))
            {
                List value = new LinkedList();
                spec = fillList(value, spec);
                list.add(value);
            }
            else
            {
                String value = spec.substring(0, 1);
                spec = drop(spec, value);
                list.add(value);
            }
            if (spec.startsWith(","))
            {
                spec = drop(spec, ",");
            }
        }
        return drop(spec, ")");
    }

    protected String drop(String spec, String delim)
    {
        assertTrue("expected " + delim + " but spec is " + spec, spec.startsWith(delim));
        return spec.substring(1);
    }

}
