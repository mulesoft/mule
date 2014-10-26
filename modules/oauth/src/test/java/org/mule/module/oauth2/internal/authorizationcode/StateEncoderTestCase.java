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
    public static final String TEST_RESOURCE_OWNER_ID = "test-oauth-id";
    public static final String TEST_ON_COMPLETE_URL = "http://host:12/path";

    @Test(expected = IllegalArgumentException.class)
    public void encodeNullOAuthStateId()
    {
        StateEncoder.encodeResourceOwnerIdInState(ORIGINAL_STATE_VALUE, null);
    }

    @Test
    public void encodeAndDecodeWithState()
    {
        final String encodedState = StateEncoder.encodeResourceOwnerIdInState(ORIGINAL_STATE_VALUE, TEST_RESOURCE_OWNER_ID);
        assertThat(StateEncoder.decodeResourceOwnerId(encodedState), is(TEST_RESOURCE_OWNER_ID));
        assertThat(StateEncoder.decodeOriginalState(encodedState), is(ORIGINAL_STATE_VALUE));
    }

    @Test
    public void encodeAndDecodeWithNullState()
    {
        final String encodedState = StateEncoder.encodeResourceOwnerIdInState(null, TEST_RESOURCE_OWNER_ID);
        assertThat(StateEncoder.decodeResourceOwnerId(encodedState), is(TEST_RESOURCE_OWNER_ID));
        assertThat(StateEncoder.decodeOriginalState(encodedState), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowNewParameterAfterOnCompleteRedirectTo()
    {
        final String encodedState = StateEncoder.encodeOnCompleteRedirectToInState(null, TEST_ON_COMPLETE_URL);
        StateEncoder.encodeResourceOwnerIdInState(encodedState, TEST_RESOURCE_OWNER_ID);
    }

    @Test
    public void encodeAndDecodeOnCompleteRedirectToParameter()
    {
        final String encodedState = StateEncoder.encodeOnCompleteRedirectToInState(null, TEST_ON_COMPLETE_URL);
        assertThat(StateEncoder.decodeOnCompleteRedirectTo(encodedState), is(TEST_ON_COMPLETE_URL));
        assertThat(StateEncoder.decodeOriginalState(encodedState), nullValue());
    }

    @Test
    public void encodeAndDecodeResourceOwnerAndOnCompleteRedirectToParameter()
    {
        String encodedState = StateEncoder.encodeResourceOwnerIdInState(ORIGINAL_STATE_VALUE, TEST_RESOURCE_OWNER_ID);
        encodedState = StateEncoder.encodeOnCompleteRedirectToInState(encodedState, TEST_ON_COMPLETE_URL);
        assertThat(StateEncoder.decodeOnCompleteRedirectTo(encodedState), is(TEST_ON_COMPLETE_URL));
        assertThat(StateEncoder.decodeResourceOwnerId(encodedState), is(TEST_RESOURCE_OWNER_ID));
        assertThat(StateEncoder.decodeOriginalState(encodedState), is(ORIGINAL_STATE_VALUE));
    }

}
