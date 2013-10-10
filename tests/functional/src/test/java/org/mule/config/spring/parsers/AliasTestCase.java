/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.AbstractBean;
import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AliasTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
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
