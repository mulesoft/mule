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

public class CollectionTestCase extends AbstractNamespaceTestCase
{

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

    public void testNamed()
    {
        assertKidsExist(1);
    }

    public void testOrphan()
    {
        assertKidsExist(2);
    }

    public void testParent()
    {
        assertKidsExist(3);
    }

}