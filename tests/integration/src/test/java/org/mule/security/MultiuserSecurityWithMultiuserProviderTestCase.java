/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;


/**
 * Tests multi-user security against a security provider which holds authentications 
 * for multiple users concurrently.
 * 
 * see EE-979
 */
public class MultiuserSecurityWithMultiuserProviderTestCase extends MultiuserSecurityTestCase
{
        
    public MultiuserSecurityWithMultiuserProviderTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "multiuser-security-test-service.xml, multiuser-security-provider.xml"},
            {ConfigVariant.FLOW, "multiuser-security-test-flow.xml, multiuser-security-provider.xml"}
        });
    }
    
    
}
