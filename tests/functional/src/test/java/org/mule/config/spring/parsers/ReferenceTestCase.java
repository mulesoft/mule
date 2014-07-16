/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertEquals;

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

public class ReferenceTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/reference-test.xml";
    }

    protected void testChildRef(int index)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        ChildBean child = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
        assertEquals("child" + index, child.getName());
    }

    @Test
    public void testNamed()
    {
        testChildRef(1);
    }

    @Test
    public void testOrphan()
    {
        testChildRef(2);
    }

    @Test
    public void testParent()
    {
        testChildRef(3);
    }
}
