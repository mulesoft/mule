/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.util.store.SimpleMemoryObjectStore;

public class AuthorizationCodeObjectStoreTestCase extends AuthorizationCodeMinimalConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-object-store-config.xml";
    }

    @Override
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        super.hitRedirectUrlAndGetToken();

        SimpleMemoryObjectStore configObjectStore = muleContext.getRegistry().get("customObjectStore");
        assertThat(configObjectStore.allKeys().size(), is(1));
        assertThat(configObjectStore.retrieve("default"), notNullValue());
    }
}
