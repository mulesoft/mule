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

public class NamedSimpleTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/named-simple-test.xml";
    }

    @Test
    public void testNamed1()
    {
        OrphanBean orphan1 = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        assertBeanPopulated(orphan1, "orphan1");
        ChildBean child1 = (ChildBean) assertContentExists(orphan1.getChild(), ChildBean.class);
        assertBeanPopulated(child1, "child1");
    }

    @Test
    public void testNamed2()
    {
        OrphanBean orphan2 = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
        assertBeanPopulated(orphan2, "orphan2");
        ChildBean child2 = (ChildBean) assertContentExists(orphan2.getChild(), ChildBean.class);
        assertBeanPopulated(child2, "child2");
    }

}
