/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.OrphanBean;

import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Automatic plurals currently do not work for attributes
 */
public class ReferenceCollectionAutoTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/reference-collection-auto-test.xml";
    }

    protected void testChildRef(int index, int size)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection kids = (Collection) assertContentExists(orphan.getKids(), Collection.class);
        assertEquals(size, kids.size());
    }

    @Test
    public void testNamed()
    {
        testChildRef(1, 2);
    }

    @Test
    public void testOrphan()
    {
        testChildRef(2, 1);
    }

    @Test
    public void testParent()
    {
        testChildRef(3, 3);
    }

}
