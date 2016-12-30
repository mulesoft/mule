/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.oauth2.internal.authorizationcode.DefaultAuthorizationCodeGrantType;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.clientcredentials.ClientCredentialsGrantType;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * An extension to hook oauth2 to http extension connectors.
 *
 * @since 4.0
 */
@Extension(name = "OAuth")
@Import(type = HttpAuthentication.class, from = "HTTP")
@Operations(OAuthOperations.class)
@SubTypeMapping(baseType = HttpAuthentication.class,
    subTypes = {DefaultAuthorizationCodeGrantType.class, ClientCredentialsGrantType.class})
@Xml(namespace = "oauth")
@Export(classes = {TokenManagerConfig.class, ResourceOwnerOAuthContext.class, TokenNotFoundException.class})
public class OAuthExtension {

}
