/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import oauth.signpost.signature.OAuthMessageSigner;
import oauth.signpost.signature.SigningStrategy;

public interface OAuth1Connector
{

    public String getAccessTokenUrl();

    public String getConsumerKey();

    public String getConsumerSecret();
    
    public String getAccessToken();
    
    public String getVerifierRegex();
    
    public String getScope();

    public String getAuthorizationUrl();
    
    public OAuthMessageSigner getMessageSigner();
    
    public SigningStrategy getSigningStrategy();
    
    
}


