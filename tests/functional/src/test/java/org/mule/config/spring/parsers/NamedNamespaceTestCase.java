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

public class NamedNamespaceTestCase extends AbstractNamespaceTestCase
{


    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/parsers-test-namespace-config.xml";
    }

    public void testNamed()
    {
        OrphanBean orphan1 = (OrphanBean) beanExists("orphan1", OrphanBean.class);
        contentExists(orphan1.getChild(), ChildBean.class);
    }
}
