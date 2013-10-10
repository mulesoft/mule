/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class DefaultThreadingProfileMule2083TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "default-threading-profile-1-mule-2083.xml, default-threading-profile-2-mule-2083.xml";
    }

    @Test
    public void testStartup()
    {
        // no-op
    }

}
