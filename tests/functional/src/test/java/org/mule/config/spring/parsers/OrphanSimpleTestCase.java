/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import org.mule.module.springconfig.parsers.beans.ChildBean;
import org.mule.module.springconfig.parsers.beans.OrphanBean;

import org.junit.Test;

public class OrphanSimpleTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
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
