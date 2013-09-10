/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import java.util.Date;
import java.util.regex.Pattern;

public interface OAuthResponseParser
{

    public String extractAccessCode(Pattern pattern, String value);
    
    public String extractRefreshToken(Pattern pattern, String value);
    
    public Date extractExpirationTime(Pattern pattern, String value);
}


