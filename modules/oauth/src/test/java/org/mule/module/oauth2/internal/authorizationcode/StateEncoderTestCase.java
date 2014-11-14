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

import org.mule.module.oauth2.internal.StateDecoder;
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
        new StateEncoder(ORIGINAL_STATE_VALUE).encodeResourceOwnerIdInState(null);
    }

    @Test
    public void encodeAndDecodeWithState()
    {
        final StateEncoder stateEncoder = new StateEncoder(ORIGINAL_STATE_VALUE);
        stateEncoder.encodeResourceOwnerIdInState(TEST_RESOURCE_OWNER_ID);
        final String encodedState = stateEncoder.getEncodedState();
        final StateDecoder stateDecoder = new StateDecoder(encodedState);
        assertThat(stateDecoder.decodeResourceOwnerId(), is(TEST_RESOURCE_OWNER_ID));
        assertThat(stateDecoder.decodeOriginalState(), is(ORIGINAL_STATE_VALUE));
    }

    @Test
    public void encodeAndDecodeWithNullState()
    {
        final StateEncoder stateEncoder = new StateEncoder(null);
        stateEncoder.encodeResourceOwnerIdInState(TEST_RESOURCE_OWNER_ID);
        final String encodedState = stateEncoder.getEncodedState();
        final StateDecoder stateDecoder = new StateDecoder(encodedState);
        assertThat(stateDecoder.decodeResourceOwnerId(), is(TEST_RESOURCE_OWNER_ID));
        assertThat(stateDecoder.decodeOriginalState(), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowNewParameterAfterOnCompleteRedirectTo()
    {
        final StateEncoder stateEncoder = new StateEncoder(null);
        stateEncoder.encodeOnCompleteRedirectToInState(TEST_ON_COMPLETE_URL);
        stateEncoder.encodeResourceOwnerIdInState(TEST_RESOURCE_OWNER_ID);
    }

    @Test
    public void encodeAndDecodeOnCompleteRedirectToParameter()
    {
        final StateEncoder stateEncoder = new StateEncoder(null);
        stateEncoder.encodeOnCompleteRedirectToInState(TEST_ON_COMPLETE_URL);
        final String encodedState = stateEncoder.getEncodedState();
        final StateDecoder stateDecoder = new StateDecoder(encodedState);
        assertThat(stateDecoder.decodeOnCompleteRedirectTo(), is(TEST_ON_COMPLETE_URL));
        assertThat(stateDecoder.decodeOriginalState(), nullValue());
    }

    @Test
    public void encodeAndDecodeResourceOwnerAndOnCompleteRedirectToParameter()
    {
        final StateEncoder stateEncoder = new StateEncoder(ORIGINAL_STATE_VALUE);
        stateEncoder.encodeResourceOwnerIdInState(TEST_RESOURCE_OWNER_ID);
        stateEncoder.encodeOnCompleteRedirectToInState(TEST_ON_COMPLETE_URL);
        String encodedState = stateEncoder.getEncodedState();
        final StateDecoder stateDecoder = new StateDecoder(encodedState);
        assertThat(stateDecoder.decodeOnCompleteRedirectTo(), is(TEST_ON_COMPLETE_URL));
        assertThat(stateDecoder.decodeResourceOwnerId(), is(TEST_RESOURCE_OWNER_ID));
        assertThat(stateDecoder.decodeOriginalState(), is(ORIGINAL_STATE_VALUE));
    }

}
