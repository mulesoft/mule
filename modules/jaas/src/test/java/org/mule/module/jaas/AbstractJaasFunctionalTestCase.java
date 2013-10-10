/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jaas;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.EncryptionStrategy;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.CryptoFailureException;
import org.mule.security.MuleCredentials;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public abstract class AbstractJaasFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public AbstractJaasFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

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
