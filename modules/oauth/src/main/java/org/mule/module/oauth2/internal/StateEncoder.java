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
 * Utility class to encode and decode an resourceOwnerId in the authentication request state parameter
 */
public class StateEncoder
{

    public static final String OAUTH_STATE_ID_PARAM_NAME = ":resourceOwnerId";
    public static final String OAUTH_STATE_ID_PARAM_ASSIGN = OAUTH_STATE_ID_PARAM_NAME + "=";

    /**
     * Creates an state value with the resource owner id encoded in it.
     *
     * @param originalState the original state
     * @param resourceOwnerId  the resourceOwnerId to encode
     * @return an updated state with the original content plus the oath state id.
     */
    public static final String encodeResourceOwnerIdInState(final String originalState, final String resourceOwnerId)
    {
        Preconditions.checkArgument(resourceOwnerId != null, "resourceOwnerId parameter cannot be null");
        String newState;
        if (originalState == null && resourceOwnerId != null)
        {
            newState = OAUTH_STATE_ID_PARAM_ASSIGN + resourceOwnerId;
        }
        else if (resourceOwnerId != null)
        {
            newState = (originalState == null ? StringUtils.EMPTY : originalState) + OAUTH_STATE_ID_PARAM_ASSIGN + resourceOwnerId;
        }
        else
        {
            newState = originalState;
        }
        return newState;
    }

    /**
     * Decodes the original state from an encoded state using #encodeResourceOwnerIdInState
     *
     * @param state the encoded state
     * @return the original state, null if the original state was empty.
     */
    public static String decodeOriginalState(final String state)
    {
        String originalState = state;
        if (state != null)
        {
            final int resourceOwnerIdSuffixIndex = state.indexOf(OAUTH_STATE_ID_PARAM_ASSIGN);
            if (resourceOwnerIdSuffixIndex != -1)
            {
                originalState = state.substring(0, resourceOwnerIdSuffixIndex);
                if (originalState.isEmpty())
                {
                    originalState = null;
                }
            }
        }
        return originalState;
    }

    /**
     * Decodes the resource owner id from an encoded state using #encodeResourceOwnerIdInState
     *
     * @param state the encoded state
     * @return the resource owner id, null if there's no oauth state id encoded in it.
     */
    public static String decodeResourceOwnerId(final String state)
    {
        String resourceOwnerId = null;
        if (state != null && state.contains(OAUTH_STATE_ID_PARAM_ASSIGN))
        {
            final int resourceOwnerIdSuffixIndex = state.indexOf(OAUTH_STATE_ID_PARAM_ASSIGN);
            resourceOwnerId = state.substring(resourceOwnerIdSuffixIndex + OAUTH_STATE_ID_PARAM_ASSIGN.length(), state.length());
        }
        return resourceOwnerId;
    }
}
