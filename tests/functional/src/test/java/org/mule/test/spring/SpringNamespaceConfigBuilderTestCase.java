/*
 * $Id:SpringNamespaceConfigBuilderTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.tck.AbstractConfigBuilderTestCase;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{
 
    public String getConfigResources()
    {
        return "org/mule/test/spring/test-xml-mule2-config.xml," +
                "org/mule/test/spring/test-xml-mule2-config-split.xml";
    }

}