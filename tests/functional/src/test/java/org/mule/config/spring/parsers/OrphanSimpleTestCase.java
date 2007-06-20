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

public class OrphanSimpleTestCase extends AbstractNamespaceTestCase
{

    public OrphanSimpleTestCase()
    {
        super("org/mule/config/spring/parsers/orphan-simple-test.xml");
    }

    public void testOrphan2()
    {
        OrphanBean orphan2 = (OrphanBean) beanExists("orphan2", OrphanBean.class);
        populated(orphan2, "orphan2");
        ChildBean child2 = (ChildBean) contentExists(orphan2.getChild(), ChildBean.class);
        populated(child2, "child2");
    }

}