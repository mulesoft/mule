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

/**
 * This constructs a <em>temporary</em> bean whose contents are injected into a parent map by
 * {@link org.mule.config.spring.parsers.assembly.DefaultBeanAssembler}.  Since this occurs
 * <em>before</em> child elements are processed this will <em>cannot</em> handle nested elements.
 */
public class MapEntryCombinerTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/map-entry-combiner-test.xml";
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

}