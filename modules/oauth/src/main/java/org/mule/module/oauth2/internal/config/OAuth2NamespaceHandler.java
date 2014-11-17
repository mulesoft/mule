/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.config;


import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.GenericChildMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.oauth2.internal.ParameterExtractor;
import org.mule.module.oauth2.internal.authorizationcode.AuthorizationRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.AutoAuthorizationCodeTokenRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.DefaultAuthorizationCodeGrantType;
import org.mule.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;
import org.mule.module.oauth2.internal.clientcredentials.ClientCredentialsGrantType;
import org.mule.module.oauth2.internal.clientcredentials.ClientCredentialsTokenRequestHandler;
import org.mule.module.oauth2.internal.tokenmanager.InvalidateOauthContextMessageProcessor;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;

public class OAuth2NamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("token-manager-config", new MuleOrphanDefinitionParser(TokenManagerConfig.class, true));
        final ChildDefinitionParser authorizationCodeGrantType = new ChildDefinitionParser("authentication", DefaultAuthorizationCodeGrantType.class);
        registerMuleBeanDefinitionParser("authorization-code-grant-type", authorizationCodeGrantType);
        registerMuleBeanDefinitionParser("authorization-request", new ChildDefinitionParser("authorizationRequestHandler", AuthorizationRequestHandler.class));
        final ParentContextDefinitionParser tokenRequestHandlerDefinitionParser = new ParentContextDefinitionParser("authorization-code-grant-type", new ChildDefinitionParser("tokenRequestHandler", AutoAuthorizationCodeTokenRequestHandler.class));
        tokenRequestHandlerDefinitionParser.otherwise(new ChildDefinitionParser("tokenRequestHandler", ClientCredentialsTokenRequestHandler.class));
        registerMuleBeanDefinitionParser("token-request", tokenRequestHandlerDefinitionParser);
        registerMuleBeanDefinitionParser("token-response", new ChildDefinitionParser("tokenResponseConfiguration", TokenResponseConfiguration.class));
        registerMuleBeanDefinitionParser("custom-parameters", new GenericChildMapDefinitionParser("customParameters", "custom-parameter", "paramName", "value"));
        registerMuleBeanDefinitionParser("custom-parameter-extractor", new ChildDefinitionParser("parameterExtractor", ParameterExtractor.class));
        registerMuleBeanDefinitionParser("client-credentials-grant-type", new ChildDefinitionParser("authentication", ClientCredentialsGrantType.class, true));
        registerMuleBeanDefinitionParser("invalidate-oauth-context", new MessageProcessorDefinitionParser(InvalidateOauthContextMessageProcessor.class));
    }

}
