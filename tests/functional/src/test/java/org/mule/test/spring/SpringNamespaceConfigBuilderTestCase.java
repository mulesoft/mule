/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.tck.AbstractConfigBuilderTestCase;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{

    public SpringNamespaceConfigBuilderTestCase()
    {
        super(false);
        setDisposeContextPerClass(true);
    }

    @Override
    public String getConfigResources()
    {
        return "org/mule/test/spring/config1/test-xml-mule2-config.xml," +
                "org/mule/test/spring/config1/test-xml-mule2-config-split.xml";
    }
}
