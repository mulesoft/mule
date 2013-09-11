/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

/**
 * Internal util class for Devkit's use only. For general purpose use cases prefer
 * standard Jersey client instead.
 */
public interface HttpUtil
{

    /**
     * It posts the given body to a url and returns the response body as a string
     * 
     * @param url the url you want to consume
     * @param body the body to post
     * @return a string with the server's response body
     */
    public String post(String url, String body);
}
