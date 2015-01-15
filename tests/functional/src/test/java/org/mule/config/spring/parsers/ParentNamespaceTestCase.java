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

public class ParentNamespaceTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/parsers-test-namespace-config.xml";
    }

    @Test
    public void testParent()
    {
        OrphanBean orphan3 = (OrphanBean) assertBeanExists("orphan3", OrphanBean.class);
        assertContentExists(orphan3.getChild(), ChildBean.class);
    }
}
