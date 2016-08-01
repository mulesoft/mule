/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

public class CertificateTokenTestCase extends UsernameTokenTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                             "org/mule/runtime/module/cxf/wssec/cxf-secure-service-flow-httpn.xml",
                             "org/mule/runtime/module/cxf/wssec/certificate-token-conf.xml"
        };
    }
}


