/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.state;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import org.mule.api.store.ListableObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.lock.LockFactory;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class ConfigOAuthContextTestCase extends AbstractMuleTestCase
{

    public static final String USER_ID = "user";
    public static final String TEST_CONFIG_NAME = "test-config-name";
    private LockFactory mockLockFactory = mock(LockFactory.class, Answers.RETURNS_DEEP_STUBS.get());
    private ListableObjectStore mockObjectStore = mock(ListableObjectStore.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    public void nonExistentUserIdReturnNewConfig() throws Exception
    {
        Mockito.when(mockObjectStore.contains(anyString())).thenReturn(false);
        assertThat(new ConfigOAuthContext(mockLockFactory, mockObjectStore, TEST_CONFIG_NAME).getContextForResourceOwner(USER_ID), notNullValue());
    }

}
