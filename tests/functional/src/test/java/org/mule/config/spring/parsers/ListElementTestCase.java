/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ListElementTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
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
