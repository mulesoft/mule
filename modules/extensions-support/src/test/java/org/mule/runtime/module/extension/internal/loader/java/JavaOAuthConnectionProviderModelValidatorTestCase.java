/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaOAuthConnectionProviderModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.oauth.TestOAuthConnection;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JavaOAuthConnectionProviderModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final JavaOAuthConnectionProviderModelValidator validator = new JavaOAuthConnectionProviderModelValidator();

  @Test
  @Issue("W-14180661")
  public void verifyExpressionsNotSupportedInOAuthParameters() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("is marked as supporting expressions. Expressions are not supported in OAuth parameters");

    Class<?> extensionClass = OAuthWithParameterSupportingExpressions.class;
    DefaultExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(
                                                                            extensionClass.getClassLoader(),
                                                                            getDefault(emptySet()));

    declarerFor(OAuthWithParameterSupportingExpressions.class, getProductVersion(), ctx);

    ExtensionModel extensionModel = new ExtensionModelFactory().create(ctx);

    validate(extensionModel, validator);
  }

  @Extension(name = "OAuth extension with parameter supporting expressions")
  @ConnectionProviders({OAuthConnectionProviderWithParameterSupportingExpressions.class})
  public static class OAuthWithParameterSupportingExpressions {
  }

  @AuthorizationCode(accessTokenUrl = "tokenUrl",
      authorizationUrl = "authorizationUrl",
      defaultScopes = "defaultScope")
  private static class OAuthConnectionProviderWithParameterSupportingExpressions
      implements ConnectionProvider<TestOAuthConnection> {

    private AuthorizationCodeState oauthState;

    @Expression(SUPPORTED)
    @OAuthParameter
    private String state;

    public OAuthConnectionProviderWithParameterSupportingExpressions() {}

    @Override
    public TestOAuthConnection connect() throws ConnectionException {
      return null;
    }

    public void disconnect(TestOAuthConnection connection) {

    }

    @Override
    public ConnectionValidationResult validate(TestOAuthConnection connection) {
      return null;
    }

  }

}
