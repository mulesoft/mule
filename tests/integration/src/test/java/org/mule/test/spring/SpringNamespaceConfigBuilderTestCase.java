/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.functional.AbstractConfigBuilderTestCase;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{
    public SpringNamespaceConfigBuilderTestCase()
    {
        super(false);
        setDisposeContextPerClass(true);
    }

    @Override
    public String[] getConfigFiles()
    {
        return new String[] {
            "org/mule/test/spring/config1/test-xml-mule2-config.xml",
            "org/mule/test/spring/config1/test-xml-mule2-config-split.xml"
        };
    }
}
