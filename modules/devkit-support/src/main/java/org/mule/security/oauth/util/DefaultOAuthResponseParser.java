/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultOAuthResponseParser implements OAuthResponseParser
{

    @Override
    public String extractAccessCode(Pattern pattern, String value)
    {
        String match = this.match(pattern, value);
        if (match != null)
        {
            try
            {
                return URLDecoder.decode(match, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException("UTF-8 encoding not supported", e);
            }

        }
        else
        {
            throw new IllegalArgumentException(String.format(
                "OAuth access token could not be extracted from: %s", value));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String extractRefreshToken(Pattern pattern, String value)
    {
        return this.match(pattern, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date extractExpirationTime(Pattern pattern, String value)
    {
        String match = this.match(pattern, value);
        if (match != null)
        {
            Long expirationSecsAhead = Long.parseLong(match);
            return new Date((System.currentTimeMillis() + (expirationSecsAhead * 1000)));
        }

        return null;
    }

    private String match(Pattern pattern, String value)
    {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() && (matcher.groupCount() >= 1) ? matcher.group(1) : null;
    }

}
