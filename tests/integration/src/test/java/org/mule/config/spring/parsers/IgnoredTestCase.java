/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertTrue;

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

public class IgnoredTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
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

    @Test
    public void testNamed()
    {
        assertIgnoredFlagUnset(1);
    }

    @Test
    public void testOrphan()
    {
        assertIgnoredFlagUnset(2);
    }

    @Test
    public void testParent()
    {
        assertIgnoredFlagUnset(3);
    }
}
