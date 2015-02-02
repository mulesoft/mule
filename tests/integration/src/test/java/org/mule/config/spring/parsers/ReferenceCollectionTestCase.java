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
 * References to collections in attributes are currently not handled correctly
 */
public class ReferenceCollectionTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/reference-collection-test.xml";
    }

    protected void testOffspringRef(int index, int size)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection<?> offspring = (Collection<?>) assertContentExists(orphan.getOffspring(), Collection.class);
        assertEquals(size, offspring.size());
    }

    @Test
    public void testNamed()
    {
        testOffspringRef(1, 2);
    }

    @Test
    public void testOrphan()
    {
        testOffspringRef(2, 1);
    }

    @Test
    public void testParent()
    {
        testOffspringRef(3, 3);
    }

}
