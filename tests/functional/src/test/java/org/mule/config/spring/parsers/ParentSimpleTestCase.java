/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

public class ParentSimpleTestCase extends AbstractNamespaceTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/parent-simple-test.xml";
    }

    public void testParent3()
    {
        OrphanBean orphan3 = (OrphanBean) assertBeanExists("orphan3", OrphanBean.class);
        assertBeanPopulated(orphan3, "orphan3");
        ChildBean child3 = (ChildBean) assertContentExists(orphan3.getChild(), ChildBean.class);
        assertBeanPopulated(child3, "child3");
    }

}