/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;

@Ignore("Broken on removing services")
public class MtomProxyTestCase extends MtomTestCase
{

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"mtom-proxy-conf.xml"},
                {"mtom-proxy-conf-httpn.xml"}
        });
    }          
}

