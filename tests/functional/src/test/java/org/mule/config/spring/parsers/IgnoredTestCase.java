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

public class IgnoredTestCase extends AbstractNamespaceTestCase
{

    public IgnoredTestCase()
    {
        super("org/mule/config/spring/parsers/ignored-test.xml");
    }

    protected void testignored(int index)
    {
        OrphanBean orphan = (OrphanBean) beanExists("orphan" + index, OrphanBean.class);
        assertTrue("orphan" + index, orphan.isIgnored());
        ChildBean child = (ChildBean) contentExists(orphan.getChild(), ChildBean.class);
        assertTrue("child" + index, child.isIgnored());
    }

    public void testNamed()
    {
        testignored(1);
    }

    public void testOrphan()
    {
        testignored(2);
    }

    public void testParent()
    {
        testignored(3);
    }

}