/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;

public class NamedSimpleTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
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
