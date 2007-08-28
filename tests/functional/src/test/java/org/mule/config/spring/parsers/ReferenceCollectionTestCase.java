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

import java.util.Collection;

/**
 * References to collections in attributes are currently not handled correctly
 */
public class ReferenceCollectionTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/reference-collection-test.xml";
    }

    protected void testOffspringRef(int index, int size)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        Collection offspring = (Collection) assertContentExists(orphan.getOffspring(), Collection.class);
        assertEquals(size, offspring.size());
    }

    public void testNamed()
    {
        testOffspringRef(1, 2);
    }

    public void testOrphan()
    {
        testOffspringRef(2, 1);
    }

    public void testParent()
    {
        testOffspringRef(3, 3);
    }

}