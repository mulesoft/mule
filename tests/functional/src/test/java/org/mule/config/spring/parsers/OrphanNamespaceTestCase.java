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

public class OrphanNamespaceTestCase extends AbstractNamespaceTestCase
{

    public OrphanNamespaceTestCase()
    {
        super("org/mule/config/spring/parsers/parsers-test-namespace-config.xml");
    }

    public void testOrphan1()
    {
        OrphanBean orphan2 = (OrphanBean) beanExists("orphan2", OrphanBean.class);
        contentExists(orphan2.getChild(), ChildBean.class);
    }

}
