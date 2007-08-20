/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

public class InheritSimpleTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/inherit-simple-test.xml";
    }

    public void testInherit()
    {
        OrphanBean orphan1 = (OrphanBean) assertBeanExists("orphan1", OrphanBean.class);
        assertBeanPopulated(orphan1, "orphan1");
        ChildBean child1 = (ChildBean) assertContentExists(orphan1.getChild(), ChildBean.class);
        assertBeanPopulated(child1, "child1");
    }

}