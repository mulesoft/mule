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

public class ParentNamespaceTestCase extends AbstractNamespaceTestCase
{

    public ParentNamespaceTestCase()
    {
        super("org/mule/config/spring/parsers/parsers-test-namespace-config.xml");
    }
    
    public void testParent()
    {
        OrphanBean orphan3 = (OrphanBean) beanExists("orphan3", OrphanBean.class);
        contentExists(orphan3.getChild(), ChildBean.class);
    }

}
