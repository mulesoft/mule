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

import static junit.framework.Assert.assertEquals;

public class CollectionTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/collection-test.xml";
    }

    protected void assertKidsExist(int index)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection kids = (Collection) assertContentExists(orphan.getKids(), Collection.class);
        assertEquals(index + 1, kids.size());
    }

    @Test
    public void testNamed()
    {
        assertKidsExist(1);
    }

    @Test
    public void testOrphan()
    {
        assertKidsExist(2);
    }

    @Test
    public void testParent()
    {
        assertKidsExist(3);
    }

}
