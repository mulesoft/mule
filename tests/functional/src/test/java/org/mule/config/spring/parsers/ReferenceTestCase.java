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

public class ReferenceTestCase extends AbstractNamespaceTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/reference-test.xml";
    }

    protected void testChildRef(int index)
    {
        OrphanBean orphan = (OrphanBean) beanExists("orphan" + index, OrphanBean.class);
        ChildBean child = (ChildBean) contentExists(orphan.getChild(), ChildBean.class);
        assertEquals("child" + index, child.getName());
    }

    public void testNamed()
    {
        testChildRef(1);
    }

    public void testOrphan()
    {
        testChildRef(2);
    }

    public void testParent()
    {
        testChildRef(3);
    }

}