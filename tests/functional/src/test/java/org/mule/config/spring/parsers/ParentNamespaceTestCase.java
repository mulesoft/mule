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

public class ParentNamespaceTestCase extends AbstractNamespaceTestCase
{

    @Override
    protected String getConfigResources()
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
