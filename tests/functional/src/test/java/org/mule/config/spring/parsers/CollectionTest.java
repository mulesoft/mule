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

public class CollectionTest extends AbstractNamespaceTestCase
{

    public CollectionTest()
    {
        super("org/mule/config/spring/parsers/collection-test.xml");
    }

    protected void testKids(int index)
    {
        OrphanBean orphan = (OrphanBean) beanExists("orphan" + index, OrphanBean.class);
        Collection kids = (Collection) contentExists(orphan.getKids(), Collection.class);
        assertEquals(index + 1, kids.size());
    }

    public void testNamed()
    {
        testKids(1);
    }

    public void testOrphan()
    {
        testKids(2);
    }

    public void testParent()
    {
        testKids(3);
    }

}