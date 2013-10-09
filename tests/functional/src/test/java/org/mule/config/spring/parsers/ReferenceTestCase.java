/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReferenceTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
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
