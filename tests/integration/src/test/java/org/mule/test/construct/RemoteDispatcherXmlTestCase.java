/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
