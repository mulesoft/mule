/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.state;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.lock.LockFactory;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class ConfigOAuthStateTestCase extends AbstractMuleTestCase
{

    public static final String USER_ID = "user";
    public static final String TEST_CONFIG_NAME = "test-config-name";
    private LockFactory mockLockFactory = Mockito.mock(LockFactory.class);

    @Test
    public void nonExistentUserIdReturnNewConfig()
    {
        assertThat(new ConfigOAuthState(mockLockFactory, TEST_CONFIG_NAME).getStateForUser(USER_ID), notNullValue());
    }

    @Test
    public void existentUserIdReturnsPreviousConfig()
    {
        final ConfigOAuthState configOAuthState = new ConfigOAuthState(mockLockFactory, TEST_CONFIG_NAME);
        final UserOAuthState userState = configOAuthState.getStateForUser(USER_ID);
        assertThat(configOAuthState.getStateForUser(USER_ID), is(userState));
    }

}
