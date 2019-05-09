/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

/**
 * Manages all the resources needed for extensions to consume OAuth providers.
 * <p>
 * Among other things, it manages access token callbacks, authorization endpoints, etc.
 *
 * @since 4.0
 */
public interface ExtensionsOAuthManager {

  AuthorizationCodeOAuthHandler getAuthorizationCodeOAuthHandler();


}
