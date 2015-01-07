/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

public class CertificateTokenTestCase extends UsernameTokenTestCase
{

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {new String[] {"org/mule/module/cxf/wssec/cxf-secure-service-flow.xml, org/mule/module/cxf/wssec/certificate-token-conf.xml"}},
                {new String[] {"org/mule/module/cxf/wssec/cxf-secure-service-flow-httpn.xml, org/mule/module/cxf/wssec/certificate-token-conf.xml"}}
        });
    }

    @Parameterized.Parameter
    public String[] configFiles;

    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
    }
}
