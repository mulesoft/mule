/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.module.oauth2.internal.StateEncoder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class StateEncoderTestCase extends AbstractMuleTestCase
{

    public static final String ORIGINAL_STATE_VALUE = "original-state-value";
    public static final String TEST_OAUTH_ID = "test-oauth-id";

    @Test(expected = IllegalArgumentException.class)
    public void encodeNullOAuthStateId()
    {
        StateEncoder.encodeResourceOwnerIdInState(ORIGINAL_STATE_VALUE, null);
    }

    @Test
    public void encodeAndDecodeWithState()
    {
        final String encodedState = StateEncoder.encodeResourceOwnerIdInState(ORIGINAL_STATE_VALUE, TEST_OAUTH_ID);
        assertThat(StateEncoder.decodeResourceOwnerId(encodedState), is(TEST_OAUTH_ID));
        assertThat(StateEncoder.decodeOriginalState(encodedState), is(ORIGINAL_STATE_VALUE));
    }

    @Test
    public void encodeAndDecodeWithNullState()
    {
        final String encodedState = StateEncoder.encodeResourceOwnerIdInState(null, TEST_OAUTH_ID);
        assertThat(StateEncoder.decodeResourceOwnerId(encodedState), is(TEST_OAUTH_ID));
        assertThat(StateEncoder.decodeOriginalState(encodedState), nullValue());
    }

}
