/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import java.util.Collection;

/**
 * Automatic plurals currently do not work for attributes
 */
public class ReferenceCollectionAutoTestCase extends AbstractNamespaceTestCase
{

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

    public void testNamed()
    {
        testChildRef(1, 2);
    }

    public void testOrphan()
    {
        testChildRef(2, 1);
    }

    public void testParent()
    {
        testChildRef(3, 3);
    }

}