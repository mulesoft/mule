/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;

import org.junit.Test;

public class PetStoreThreadingProfileTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "petstore-threading-profile.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnector.class};
    }

    @Test
    public void customThreadingProfile() throws Exception
    {
        PetStoreConnector connector = ExtensionsTestUtils.getConfigurationFromRegistry("customThreadingProfile", getTestEvent(""));
        assertThreadingProfile(connector.getThreadingProfile());
    }

    @Test
    public void noThreadingProfile() throws Exception
    {
        PetStoreConnector connector = ExtensionsTestUtils.getConfigurationFromRegistry("noThreadingProfile", getTestEvent(""));
        assertThat(connector.getThreadingProfile(), is(nullValue()));
    }

    @Test
    public void threadingProfileOnConnection() throws Exception
    {
        PetStoreClient client = (PetStoreClient) runFlow("getClient").getMessage().getPayload();
        assertThreadingProfile(client.getThreadingProfile());
    }

    private void assertThreadingProfile(ThreadingProfile threadingProfile){
        assertThat(threadingProfile, is(notNullValue()));
        assertThat(threadingProfile.getMaxThreadsActive(), is(20));
        assertThat(threadingProfile.isDoThreading(), is(false));
    }

}
