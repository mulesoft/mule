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
 * State parameter encoder. Allows to encode and decode an resourceOwnerId in the authentication request state parameter.
 */
public class StateEncoder
{

    public static final String RESOURCE_OWNER_PARAM_NAME = ":resourceOwnerId";
    public static final String RESOURCE_OWNER_PARAM_NAME_ASSIGN = RESOURCE_OWNER_PARAM_NAME + "=";
    public static final String ON_COMPLETE_REDIRECT_TO_PARAM_NAME = ":onCompleteRedirectTo";
    public static final String ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN = ON_COMPLETE_REDIRECT_TO_PARAM_NAME + "=";
    private String state;

    public StateEncoder(String originalState)
    {
        this.state = originalState;
    }

    /**
     * Creates an state value with the resource owner id encoded in it.
     *
     * @param resourceOwnerId the resourceOwnerId to encode
     * @return an updated state with the original content plus the oath state id.
     */
    public void encodeResourceOwnerIdInState(final String resourceOwnerId)
    {
        encodeParameter(resourceOwnerId, RESOURCE_OWNER_PARAM_NAME_ASSIGN);
    }

    /**
     * Creates and state value with the redirect url to send the user when the oauth dance is complete
     *
     * @param onCompleteRedirectToValue the redirect url to encode
     * @return an updated state with the original content plus the redirect url.
     */
    public void encodeOnCompleteRedirectToInState(String onCompleteRedirectToValue)
    {
        encodeParameter(onCompleteRedirectToValue, ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN);
    }

    private void encodeParameter(String parameterValue, String parameterAssignation)
    {
        Preconditions.checkArgument(parameterValue != null, "parameter cannot be null");
        Preconditions.checkArgument(state == null ? true : !state.contains(ON_COMPLETE_REDIRECT_TO_PARAM_NAME_ASSIGN), "parameter cannot be added after " + ON_COMPLETE_REDIRECT_TO_PARAM_NAME);
        if (state == null && parameterValue != null)
        {
            state = parameterAssignation + parameterValue;
        }
        else if (parameterValue != null)
        {
            state = (state == null ? StringUtils.EMPTY : state) + parameterAssignation + parameterValue;
        }
    }

    public String getEncodedState()
    {
        return state;
    }
}
