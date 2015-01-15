/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import org.mule.module.springconfig.parsers.beans.OrphanBean;

import org.junit.Test;

public class MapCombinerTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/map-combiner-test.xml";
    }

    @Test
    public void testProperties()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("checkProps", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        assertMapEntryExists(bean.getMap(), "0", 0);
    }

    @Test
    public void testCombinedMap()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        for (int i = 0; i < 6; ++i)
        {
            assertMapEntryExists(bean.getMap(), Integer.toString(i+1), i+1);
        }
    }

    @Test
    public void testReverersedOrder()
    {
        OrphanBean bean = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
        logger.info("Map size: " + bean.getMap().size());
        for (int i = 0; i < 2; ++i)
        {
            assertMapEntryExists(bean.getMap(), Integer.toString(i+1), i+1);
        }
    }
}
