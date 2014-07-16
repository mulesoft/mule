/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static junit.framework.Assert.assertEquals;

import org.mule.config.spring.parsers.beans.OrphanBean;

import java.util.Collection;

import org.junit.Test;

public class CollectionTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/collection-test.xml";
    }

    protected void assertKidsExist(int index)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection<?> kids = (Collection<?>) assertContentExists(orphan.getKids(), Collection.class);
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
