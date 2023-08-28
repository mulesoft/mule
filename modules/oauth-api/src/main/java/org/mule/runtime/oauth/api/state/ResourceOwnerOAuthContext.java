/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.oauth.api.state;

import org.mule.api.annotation.NoImplement;

/**
 * Provides compatibility with version 1.x of the mule-oauth-client, which is a transitive api of the service api.
 * 
 * @deprecated since 1.5, use {@link org.mule.oauth.client.api.state.ResourceOwnerOAuthContext} from
 *             {@code mule-oauth-client 2.x}.
 */
@Deprecated
@NoImplement
public interface ResourceOwnerOAuthContext extends org.mule.oauth.client.api.state.ResourceOwnerOAuthContext {

}
