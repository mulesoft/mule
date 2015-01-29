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

public class NamedNamespaceTestCase extends AbstractNamespaceTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/parsers-test-namespace-config.xml";
    }

    @Test
    public void testNamed()
    {
        OrphanBean orphan1 = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        assertContentExists(orphan1.getChild(), ChildBean.class);
    }
}
