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

import static org.junit.Assert.assertTrue;

public class IgnoredTestCase extends AbstractNamespaceTestCase
{

    @Override
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
