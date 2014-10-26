/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.config;


import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.GenericChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.oauth2.internal.AuthenticationCodeAuthenticate;
import org.mule.module.oauth2.internal.AuthorizationCodeConfig;
import org.mule.module.oauth2.internal.AuthorizationRequestHandler;
import org.mule.module.oauth2.internal.AutoTokenRequestHandler;
import org.mule.module.oauth2.internal.CustomTokenRequestHandler;
import org.mule.module.oauth2.internal.ParameterExtractor;
import org.mule.module.oauth2.internal.StoreAuthenticationCodeStateMessageProcessor;
import org.mule.module.oauth2.internal.TokenResponseConfiguration;

public class OAuthNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        final MuleOrphanDefinitionParser authorizationCodeConfigParser = new MuleOrphanDefinitionParser(AuthorizationCodeConfig.class, true);
        authorizationCodeConfigParser.addReference("requestConfig");
        authorizationCodeConfigParser.addReference("listenerConfig");
        registerMuleBeanDefinitionParser("authorization-code-config", authorizationCodeConfigParser);
        registerMuleBeanDefinitionParser("authorization-request", new ChildDefinitionParser("authorizationRequestHandler", AuthorizationRequestHandler.class));
        registerMuleBeanDefinitionParser("token-request", new ChildDefinitionParser("tokenRequestHandler", AutoTokenRequestHandler.class));
        final ChildDefinitionParser customTokenRequest = new ChildDefinitionParser("tokenRequestHandler", CustomTokenRequestHandler.class);
        customTokenRequest.addReference("tokenUrlCallFlow");
        customTokenRequest.addReference("refreshTokenFlow");
        registerMuleBeanDefinitionParser("custom-token-request", customTokenRequest);
        registerMuleBeanDefinitionParser("token-response", new ChildDefinitionParser("tokenResponseConfiguration", TokenResponseConfiguration.class));
        registerMuleBeanDefinitionParser("custom-parameters", new GenericChildMapDefinitionParser("customParameters", "custom-parameter", "paramName", "value"));
        registerMuleBeanDefinitionParser("custom-parameter-extractor", new ChildDefinitionParser("parameterExtractor", ParameterExtractor.class));
        final ChildDefinitionParser authenticationDefinitionParser = new ChildDefinitionParser("auth", AuthenticationCodeAuthenticate.class);
        authenticationDefinitionParser.addReference("config");
        registerMuleBeanDefinitionParser("authorization-code-authentication", authenticationDefinitionParser);
        final MessageProcessorDefinitionParser storeAuthorizationCodeState = new MessageProcessorDefinitionParser(StoreAuthenticationCodeStateMessageProcessor.class);
        storeAuthorizationCodeState.addReference("config");
        registerMuleBeanDefinitionParser("store-authorization-code-state", storeAuthorizationCodeState);
    }

}
