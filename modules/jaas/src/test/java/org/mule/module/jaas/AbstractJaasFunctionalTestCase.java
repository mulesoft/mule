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
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJaasFunctionalTestCase extends FunctionalTestCase
{

    protected Map<String, Object> createMessagePropertiesWithCredentials(String username, String password)
        throws CryptoFailureException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        String header = createEncryptedHeader(username, password);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        return props;
    }

    private String createEncryptedHeader(String username, String password) throws CryptoFailureException
    {
        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        return MuleCredentials.createHeader(username, password, "PBE", strategy);
    }
}
