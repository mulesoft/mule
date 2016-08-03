/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class DefaultThreadingProfileMule2083TestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "default-threading-profile-1-mule-2083.xml",
            "default-threading-profile-2-mule-2083.xml"
        };
    }

    @Test
    public void testStartup()
    {
        // no-op
    }
}
