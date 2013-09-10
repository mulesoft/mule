/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import org.mule.tck.size.SmallTest;

import java.util.Date;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;

@SmallTest
public class DefaultOAuthResponseParserTestCase
{

    private static final String response = "{\n"
                                           + "\"access_token\" : \"ya29.AHES6ZTtm7SuokEB-RGtbBty9IIlNiP9-eNMMQKtXdMP3sfjL1Fc\"\n,"
                                           + "\"token_type\" : \"Bearer\",\n"
                                           + "\"expires_in\" : 3600,\n"
                                           + "\"refresh_token\" : \"1/HKSmLFXzqP0leUihZp2xUt3-5wkU7Gmu2Os_eBnzw74\"\n"
                                           + "}";

    private final static Pattern ACCESS_CODE_PATTERN = Pattern.compile("\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\"");
    private final static Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refresh_token\"[ ]*:[ ]*\"([^\\\"]*)\"");
    private final static Pattern EXPIRATION_TIME_PATTERN = Pattern.compile("\"expires_in\"[ ]*:[ ]*([\\d]*)");

    private OAuthResponseParser parser = new DefaultOAuthResponseParser();

    @Test
    public void accessToken()
    {
        Assert.assertEquals(parser.extractAccessCode(ACCESS_CODE_PATTERN, response),
            "ya29.AHES6ZTtm7SuokEB-RGtbBty9IIlNiP9-eNMMQKtXdMP3sfjL1Fc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noAccessToken()
    {
        this.parser.extractAccessCode(ACCESS_CODE_PATTERN, "i have no token for you");
    }

    @Test
    public void refreshToken()
    {
        Assert.assertEquals(parser.extractRefreshToken(REFRESH_TOKEN_PATTERN, response),
            "1/HKSmLFXzqP0leUihZp2xUt3-5wkU7Gmu2Os_eBnzw74");
    }
    
    @Test
    public void noRefreshToken() {
        Assert.assertNull(parser.extractRefreshToken(REFRESH_TOKEN_PATTERN, "no refresh token for you"));
    }
    
    @Test
    public void expirationTime()
    {
        Date now = new Date();
        Date expiration = parser.extractExpirationTime(EXPIRATION_TIME_PATTERN, response);
        Assert.assertTrue((expiration.getTime() - now.getTime()) >= (3600 * 1000));
    }
    
    @Test
    public void noExpirationTime() {
        Assert.assertNull(this.parser.extractExpirationTime(EXPIRATION_TIME_PATTERN, "you're out of time"));
    }

}
