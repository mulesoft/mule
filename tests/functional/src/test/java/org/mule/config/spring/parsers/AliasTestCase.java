/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.module.springconfig.parsers.beans.AbstractBean;
import org.mule.module.springconfig.parsers.beans.ChildBean;
import org.mule.module.springconfig.parsers.beans.OrphanBean;

import org.junit.Test;

public class AliasTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/alias-test.xml";
    }

    protected void assertFooExists(int index)
    {
        OrphanBean orphan = (OrphanBean) assertBeanExists("orphan" + index, OrphanBean.class);
        assertFooExists(orphan, 10 * index + 1);
        ChildBean child = (ChildBean) assertContentExists(orphan.getChild(), ChildBean.class);
        assertFooExists(child, 10 * index + 2);
    }

    protected void assertFooExists(AbstractBean bean, int value)
    {
        assertNotNull(bean);
        assertEquals(value, bean.getFoo());
    }

    @Test
    public void testNamed()
    {
        assertFooExists(1);
    }

    @Test
    public void testOrphan()
    {
        assertFooExists(2);
    }

    @Test
    public void testParent()
    {
        assertFooExists(3);
    }

}
