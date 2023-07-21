/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.oauth.api;

/**
 * Provides compatibility with version 1.x of the mule-oauth-client, which is a transitive api of the service api.
 * 
 * @deprecated since 1.5, use {@link org.mule.oauth.client.api.AuthorizationCodeRequest} from {@code mule-oauth-client 2.x}.
 */
@Deprecated
public interface AuthorizationCodeRequest extends org.mule.oauth.client.api.AuthorizationCodeRequest {

}
