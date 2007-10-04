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

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

public class IgnoredTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/ignored-test.xml";
    }

    protected void assertIgnoredFlagUnset(int index)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        assertTrue("orphan" + index, orphan.isIgnored());
        ChildBean child = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
        assertTrue("child" + index, child.isIgnored());
    }

    public void testNamed()
    {
        assertIgnoredFlagUnset(1);
    }

    public void testOrphan()
    {
        assertIgnoredFlagUnset(2);
    }

    public void testParent()
    {
        assertIgnoredFlagUnset(3);
    }

}