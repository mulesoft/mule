/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import org.mule.api.EncryptionStrategy;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CryptoFailureException;
import org.mule.security.MuleCredentials;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJaasFunctionalTestCase extends FunctionalTestCase
{

    protected SecurityHeader createSecurityHeader(String username, String password)
        throws CryptoFailureException
    {
        String header = createEncryptedHeader(username, password);
        return new SecurityHeader(MuleProperties.MULE_USER_PROPERTY, header);
    }

    private String createEncryptedHeader(String username, String password) throws CryptoFailureException
    {
        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        return MuleCredentials.createHeader(username, password, "PBE", strategy);
    }

    public static class SecurityHeader {

        private String key;
        private String value;

        public SecurityHeader(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }
}
