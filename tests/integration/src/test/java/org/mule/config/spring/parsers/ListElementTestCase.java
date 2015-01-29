/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

public class ListElementTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/list-element-test.xml";
    }

    @Test
    public void testListElement1()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        assertNotNull(orphan.getKids());
        assertTrue(orphan.getKids().size() == 2);
        assertTrue(orphan.getKids().contains("kid1"));
        assertTrue(orphan.getKids().contains("kid2"));
    }

    @Test
    public void testListElement2()
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
        assertNotNull(orphan.getKids());
        assertTrue(orphan.getKids().size() == 2);
        assertTrue(orphan.getKids().contains("kid1"));
        assertTrue(orphan.getKids().contains("kid2"));
    }

    // simpler list element parser doesn't support dynamic attribute
//    @Test
//    public void testListElement3()
//    {
//        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan3", OrphanBean.class);
//        assertNotNull(orphan.getKids());
//        assertTrue(orphan.getKids().size() == 3);
//        assertTrue(orphan.getKids().contains("kid1"));
//        assertTrue(orphan.getKids().contains("kid2"));
//        assertTrue(orphan.getKids().contains("kid3"));
//    }

}
