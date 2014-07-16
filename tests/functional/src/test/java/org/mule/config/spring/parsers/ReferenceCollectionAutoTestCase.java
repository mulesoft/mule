/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertEquals;

import org.mule.config.spring.parsers.beans.OrphanBean;

import java.util.Collection;

import org.junit.Test;

/**
 * Automatic plurals currently do not work for attributes
 */
public class ReferenceCollectionAutoTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/reference-collection-auto-test.xml";
    }

    protected void testChildRef(int index, int size)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection<?> kids = (Collection<?>) assertContentExists(orphan.getKids(), Collection.class);
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
