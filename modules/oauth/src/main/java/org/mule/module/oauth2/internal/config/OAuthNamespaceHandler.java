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
import org.mule.module.oauth2.internal.authorizationcode.AuthenticationCodeAuthenticate;
import org.mule.module.oauth2.internal.authorizationcode.AuthorizationCodeConfig;
import org.mule.module.oauth2.internal.authorizationcode.AuthorizationRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.AutoAuthorizationCodeTokenRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.CustomAuthorizationCodeTokenRequestHandler;
import org.mule.module.oauth2.internal.ParameterExtractor;
import org.mule.module.oauth2.internal.authorizationcode.StoreAuthenticationCodeStateMessageProcessor;
import org.mule.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;
import org.mule.module.oauth2.internal.clientcredentials.ClientCredentialsAuthenticate;
import org.mule.module.oauth2.internal.clientcredentials.ClientCredentialsConfig;
import org.mule.module.oauth2.internal.clientcredentials.ClientCredentialsTokenRequestHandler;

public class OAuthNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        final MuleOrphanDefinitionParser authorizationCodeConfigParser = new MuleOrphanDefinitionParser(AuthorizationCodeConfig.class, true);
        authorizationCodeConfigParser.addReference("requestConfig");
        authorizationCodeConfigParser.addReference("listenerConfig");
        registerMuleBeanDefinitionParser("authorization-code-config", authorizationCodeConfigParser);
        registerMuleBeanDefinitionParser("authorization-request", new ChildDefinitionParser("authorizationRequestHandler", AuthorizationRequestHandler.class));
        final ParentContextDefinitionParser tokenRequestHandlerDefinitionParser = new ParentContextDefinitionParser("authorization-code-config", new ChildDefinitionParser("tokenRequestHandler", AutoAuthorizationCodeTokenRequestHandler.class));
        tokenRequestHandlerDefinitionParser.otherwise(new ChildDefinitionParser("tokenRequestHandler", ClientCredentialsTokenRequestHandler.class));
        registerMuleBeanDefinitionParser("token-request", tokenRequestHandlerDefinitionParser);
        final ChildDefinitionParser customTokenRequest = new ChildDefinitionParser("tokenRequestHandler", CustomAuthorizationCodeTokenRequestHandler.class);
        customTokenRequest.addReference("tokenUrlCallFlow");
        customTokenRequest.addReference("refreshTokenFlow");
        registerMuleBeanDefinitionParser("custom-token-request", customTokenRequest);
        registerMuleBeanDefinitionParser("token-response", new ChildDefinitionParser("tokenResponseConfiguration", TokenResponseConfiguration.class));
        registerMuleBeanDefinitionParser("custom-parameters", new GenericChildMapDefinitionParser("customParameters", "custom-parameter", "paramName", "value"));
        registerMuleBeanDefinitionParser("custom-parameter-extractor", new ChildDefinitionParser("parameterExtractor", ParameterExtractor.class));
        final ChildDefinitionParser authorizationCodeAuthentication = new ChildDefinitionParser("auth", AuthenticationCodeAuthenticate.class);
        authorizationCodeAuthentication.addReference("config");
        registerMuleBeanDefinitionParser("authorization-code-authentication", authorizationCodeAuthentication);
        final MessageProcessorDefinitionParser storeAuthorizationCodeState = new MessageProcessorDefinitionParser(StoreAuthenticationCodeStateMessageProcessor.class);
        storeAuthorizationCodeState.addReference("config");
        registerMuleBeanDefinitionParser("store-authorization-code-state", storeAuthorizationCodeState);

        registerMuleBeanDefinitionParser("client-credentials-config", new MuleOrphanDefinitionParser(ClientCredentialsConfig.class, true));
        final ChildDefinitionParser clientCredentialsAuthenticationDefinitionParser = new ChildDefinitionParser("auth", ClientCredentialsAuthenticate.class);
        clientCredentialsAuthenticationDefinitionParser.addReference("config");
        registerMuleBeanDefinitionParser("client-credentials-authentication", clientCredentialsAuthenticationDefinitionParser);

    }

}
