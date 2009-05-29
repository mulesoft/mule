/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;


/**
 * Tests multi-user security against a security provider which holds authentications 
 * for multiple users concurrently.
 * 
 * see EE-979
 */
public class MultiuserSecurityWithMultiuserProviderTestCase extends MultiuserSecurityTestCase
{
    protected String getConfigResources()
    {
        return "multiuser-security-test.xml, multiuser-security-provider.xml";
    }
}
