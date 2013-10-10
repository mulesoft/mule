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

public class OrphanSimpleTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/orphan-simple-test.xml";
    }

    @Test
    public void testOrphan2()
    {
        OrphanBean orphan2 = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
        assertBeanPopulated(orphan2, "orphan2");
        ChildBean child2 = (ChildBean) assertContentExists(orphan2.getChild(), ChildBean.class);
        assertBeanPopulated(child2, "child2");
    }

}
