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

    public static final String RESOURCE_OWNER_PARAM_NAME = ":resourceOwnerId";
    public static final String RESOURCE_OWNER_PARAM_NAME_ASSIGN = RESOURCE_OWNER_PARAM_NAME + "=";
    public static final String ON_COMPLETE_REDIRECT_TO_PARAM_NAME = ":onCompleteRedirectTo";
    public static final String ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN = ON_COMPLETE_REDIRECT_TO_PARAM_NAME + "=";

    /**
     * Creates an state value with the resource owner id encoded in it.
     *
     * @param originalState   the original state
     * @param resourceOwnerId the resourceOwnerId to encode
     * @return an updated state with the original content plus the oath state id.
     */
    public static final String encodeResourceOwnerIdInState(final String originalState, final String resourceOwnerId)
    {
        return encodeParameter(originalState, resourceOwnerId, RESOURCE_OWNER_PARAM_NAME_ASSIGN);
    }

    /**
     * Creates and state value with the redirect url to send the user when the oauth dance is complete
     *
     * @param originalState the original state
     * @param onCompleteRedirectToValue the redirect url to encode
     * @return an updated state with the original content plus the redirect url.
     */
    public static String encodeOnCompleteRedirectToInState(String originalState, String onCompleteRedirectToValue)
    {
        return encodeParameter(originalState, onCompleteRedirectToValue, ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
    }

    private static String encodeParameter(String originalState, String parameterValue, String parameterAssignation)
    {
        Preconditions.checkArgument(parameterValue != null, "parameter cannot be null");
        Preconditions.checkArgument(originalState == null ? true : !originalState.contains(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN), "parameter cannot be added after " + ON_COMPLETE_REDIRECT_TO_PARAM_NAME);
        String newState;
        if (originalState == null && parameterValue != null)
        {
            newState = parameterAssignation + parameterValue;
        }
        else if (parameterValue != null)
        {
            newState = (originalState == null ? StringUtils.EMPTY : originalState) + parameterAssignation + parameterValue;
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
            final int indexOfResourceOwnerParameter = state.indexOf(RESOURCE_OWNER_PARAM_NAME_ASSIGN);
            final int resourceOwnerIdSuffixIndex = indexOfResourceOwnerParameter != -1 ? indexOfResourceOwnerParameter : state.indexOf(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
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
        return decodeParameter(state, RESOURCE_OWNER_PARAM_NAME_ASSIGN);
    }

    /**
     * Decodes the resource owner id from an encoded state using #encodeResourceOwnerIdInState
     *
     * @param state the encoded state
     * @return the resource owner id, null if there's no oauth state id encoded in it.
     */
    public static String decodeOnCompleteRedirectTo(final String state)
    {
        String parameterValue = null;
        if (state != null && state.contains(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN))
        {
            final int onCompleteRedirectToSuffixIndex = state.indexOf(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
            parameterValue = state.substring(onCompleteRedirectToSuffixIndex + ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN.length(), state.length());
        }
        return parameterValue;
    }

    private static String decodeParameter(String state, String parameterNameAssignation)
    {
        String parameterValue = null;
        if (state != null && state.contains(parameterNameAssignation))
        {
            final int resourceOwnerIdSuffixIndex = state.indexOf(parameterNameAssignation);
            final String stateCustomParameters = state.substring(resourceOwnerIdSuffixIndex + parameterNameAssignation.length(), state.length());
            final int onCompleteRedirectToParamIndex = stateCustomParameters.indexOf(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
            int endIndex = onCompleteRedirectToParamIndex != -1 ? onCompleteRedirectToParamIndex : stateCustomParameters.length();
            parameterValue = stateCustomParameters.substring(0, endIndex);
        }
        return parameterValue;
    }
}
