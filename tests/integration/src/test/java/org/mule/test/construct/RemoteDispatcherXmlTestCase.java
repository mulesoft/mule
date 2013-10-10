/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Test remote dispatcher using xml wire format
 */
public class RemoteDispatcherXmlTestCase extends RemoteDispatcherTestCase
{
    public RemoteDispatcherXmlTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/construct/remote-dispatcher-xml.xml"}

        });
    }
}
