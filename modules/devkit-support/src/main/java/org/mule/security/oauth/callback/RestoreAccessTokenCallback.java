/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

public interface RestoreAccessTokenCallback
{

    /**
     * Restore access token and secret
     */
    void restoreAccessToken();

    /**
     * Retrieve the just restored access token
     * 
     * @return A string representing the access token
     */
    String getAccessToken();

    /**
     * Retrieve the access token secret
     * 
     * @return A string representing the access token secret
     */
    String getAccessTokenSecret();
}
