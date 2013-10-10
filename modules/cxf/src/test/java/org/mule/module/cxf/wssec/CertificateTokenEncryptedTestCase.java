/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

public class CertificateTokenEncryptedTestCase extends CertificateTokenTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wssec/cxf-secure-service.xml, org/mule/module/cxf/wssec/certificate-token-encrypted-conf.xml";
    }
}


