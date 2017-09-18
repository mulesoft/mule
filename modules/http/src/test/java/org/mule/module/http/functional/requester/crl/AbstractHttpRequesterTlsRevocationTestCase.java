/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester.crl;

import static java.util.Arrays.asList;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractHttpRequesterTlsRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{

    AbstractHttpRequesterTlsRevocationTestCase(String configFile, String crlPath, String entityCertified)
    {
        super(configFile, crlPath, entityCertified);
    }

    @Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {
                "http-requester-tls-revocation-file-config.xml",
                "http-requester-tls-revocation-crl-standard-config.xml"
        });
    }

}
