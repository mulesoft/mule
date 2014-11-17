/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

/**
 * Decoder for the oauth state. Allows to retrieve the parameters encoded in an state and to retrieve the original state.
 */
public class StateDecoder
{

    private String state;

    /**
     * @param state the raw state or null if there's no state.
     */
    public StateDecoder(String state)
    {
        this.state = state;
    }

    /**
     * Decodes the original state from an encoded state using {@link org.mule.module.oauth2.internal.StateEncoder#encodeResourceOwnerIdInState}
     *
     * @return the original state, null if the original state was empty.
     */
    public String decodeOriginalState()
    {
        String originalState = state;
        if (state != null)
        {
            final int indexOfResourceOwnerParameter = state.indexOf(StateEncoder.RESOURCE_OWNER_PARAM_NAME_ASSIGN);
            final int resourceOwnerIdSuffixIndex = indexOfResourceOwnerParameter != -1 ? indexOfResourceOwnerParameter : state.indexOf(StateEncoder.ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
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
     * @return the resource owner id, null if there's no oauth state id encoded in it.
     */
    public String decodeResourceOwnerId()
    {
        return decodeParameter(StateEncoder.RESOURCE_OWNER_PARAM_NAME_ASSIGN);
    }

    /**
     * Decodes the resource owner id from an encoded state using {@link org.mule.module.oauth2.internal.StateEncoder#encodeResourceOwnerIdInState}
     *
     * @return the resource owner id, null if there's no oauth state id encoded in it.
     */
    public String decodeOnCompleteRedirectTo()
    {
        String parameterValue = null;
        if (state != null && state.contains(StateEncoder.ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN))
        {
            final int onCompleteRedirectToSuffixIndex = state.indexOf(StateEncoder.ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
            parameterValue = state.substring(onCompleteRedirectToSuffixIndex + StateEncoder.ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN.length(), state.length());
        }
        return parameterValue;
    }

    private String decodeParameter(String parameterNameAssignation)
    {
        String parameterValue = null;
        if (state != null && state.contains(parameterNameAssignation))
        {
            final int resourceOwnerIdSuffixIndex = state.indexOf(parameterNameAssignation);
            final String stateCustomParameters = state.substring(resourceOwnerIdSuffixIndex + parameterNameAssignation.length(), state.length());
            final int onCompleteRedirectToParamIndex = stateCustomParameters.indexOf(StateEncoder.ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
            int endIndex = onCompleteRedirectToParamIndex != -1 ? onCompleteRedirectToParamIndex : stateCustomParameters.length();
            parameterValue = stateCustomParameters.substring(0, endIndex);
        }
        return parameterValue;
    }

}
