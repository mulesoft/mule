/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
