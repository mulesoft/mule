/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.util.Preconditions;
import org.mule.util.StringUtils;

/**
 * Utility class to encode and decode an OAuthStateId in the authentication request state parameter
 */
public class StateEncoder
{

    public static final String OAUTH_STATE_ID_PARAM_NAME = ":oauthStateId";
    public static final String OAUTH_STATE_ID_PARAM_ASSIGN = OAUTH_STATE_ID_PARAM_NAME + "=";

    /**
     * Creates an state value with the oauth state id encoded in it.
     *
     * @param originalState the original state
     * @param oauthStateId  the oauthStateId to encode
     * @return an updated state with the original content plus the oath state id.
     */
    public static final String encodeOAuthStateIdInState(final String originalState, final String oauthStateId)
    {
        Preconditions.checkArgument(oauthStateId != null, "oauthStateId parameter cannot be null");
        String newState;
        if (originalState == null && oauthStateId != null)
        {
            newState = OAUTH_STATE_ID_PARAM_ASSIGN + oauthStateId;
        }
        else if (oauthStateId != null)
        {
            newState = (originalState == null ? StringUtils.EMPTY : originalState) + OAUTH_STATE_ID_PARAM_ASSIGN + oauthStateId;
        }
        else
        {
            newState = originalState;
        }
        return newState;
    }

    /**
     * Decodes the original state from an encoded state using #encodeOAuthStateIdInState
     *
     * @param state the encoded state
     * @return the original state, null if the original state was empty.
     */
    public static String decodeOriginalState(final String state)
    {
        String originalState = state;
        if (state != null)
        {
            final int oauthStateIdSuffixIndex = state.indexOf(OAUTH_STATE_ID_PARAM_ASSIGN);
            if (oauthStateIdSuffixIndex != -1)
            {
                originalState = state.substring(0, oauthStateIdSuffixIndex);
                if (originalState.isEmpty())
                {
                    originalState = null;
                }
            }
        }
        return originalState;
    }

    /**
     * Decodes the oauth state id from an encoded state using #encodeOAuthStateIdInState
     *
     * @param state the encoded state
     * @return the oauth state id, null if there's no oauth state id encoded in it.
     */
    public static String decodeOAuthStateId(final String state)
    {
        String oauthStateId = null;
        if (state != null && state.contains(OAUTH_STATE_ID_PARAM_ASSIGN))
        {
            final int oauthStateIdSuffixIndex = state.indexOf(OAUTH_STATE_ID_PARAM_ASSIGN);
            oauthStateId = state.substring(oauthStateIdSuffixIndex + OAUTH_STATE_ID_PARAM_ASSIGN.length(), state.length());
        }
        return oauthStateId;
    }
}
