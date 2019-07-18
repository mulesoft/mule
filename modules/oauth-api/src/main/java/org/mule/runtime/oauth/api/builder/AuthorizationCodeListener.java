/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

/**
 * Allows to get notified about certain events related to an OAuth dance with Authorization Code grant type
 * @since 4.2.0
 * @deprecated since 4.2.2. Use {@link org.mule.runtime.oauth.api.listener.AuthorizationCodeListener} instead
 */
@Deprecated
public interface AuthorizationCodeListener extends org.mule.runtime.oauth.api.listener.AuthorizationCodeListener {

}
