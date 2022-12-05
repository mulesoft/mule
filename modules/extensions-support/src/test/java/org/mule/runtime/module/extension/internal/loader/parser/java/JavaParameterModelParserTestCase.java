/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forConnectionProvider;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils.FIRST_MULE_VERSION;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FieldWrapper;

import java.util.Optional;

import org.junit.Test;

public class JavaParameterModelParserTestCase {

  private static final String ALIAS = "alias";
  private static final HttpParameterPlacement PARAMETER_PLACEMENT = HttpParameterPlacement.HEADERS;

  @Test
  public void nonOAuthParameter() throws Exception {
    Optional<OAuthParameterModelProperty> oAuthParameterModelProperty =
        getOAuthParameterModelPropertyFromParameterName("parameter");
    assertThat(oAuthParameterModelProperty.isPresent(), is(false));
  }

  @Test
  public void oAuthParameter() throws Exception {
    Optional<OAuthParameterModelProperty> oAuthParameterModelProperty =
        getOAuthParameterModelPropertyFromParameterName("oAuthParameter");
    assertThat(oAuthParameterModelProperty.isPresent(), is(true));
    assertThat(oAuthParameterModelProperty.get().getPlacement(), is(PARAMETER_PLACEMENT));
    assertThat(oAuthParameterModelProperty.get().getRequestAlias(), is(ALIAS));
  }

  @Test
  public void sdkOAuthParameter() throws Exception {
    Optional<OAuthParameterModelProperty> oAuthParameterModelProperty =
        getOAuthParameterModelPropertyFromParameterName("sdkOAuthParameter");
    assertThat(oAuthParameterModelProperty.isPresent(), is(true));
    assertThat(oAuthParameterModelProperty.get().getPlacement(), is(PARAMETER_PLACEMENT));
    assertThat(oAuthParameterModelProperty.get().getRequestAlias(), is(ALIAS));
  }

  @Test
  public void minMuleVersionParameter() throws Exception {
    ExtensionParameter extensionParameter = new FieldWrapper(TestConnectionProvider.class
        .getField("parameter"), new DefaultExtensionsTypeLoaderFactory()
            .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    JavaParameterModelParser javaParameterModelParser =
        new JavaParameterModelParser(extensionParameter, Optional.empty(), forConnectionProvider("TestConnectionProvider"));
    Optional<MuleVersion> minMuleVersion = javaParameterModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(false));
  }

  @Test
  public void minMuleVersionSdkParameter() throws Exception {
    ExtensionParameter extensionParameter = new FieldWrapper(TestConnectionProvider.class
        .getField("sdkOAuthParameter"), new DefaultExtensionsTypeLoaderFactory()
            .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    JavaParameterModelParser javaParameterModelParser =
        new JavaParameterModelParser(extensionParameter, Optional.empty(), forConnectionProvider("TestConnectionProvider"));
    Optional<MuleVersion> minMuleVersion = javaParameterModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(false));
  }

  private Optional<OAuthParameterModelProperty> getOAuthParameterModelPropertyFromParameterName(String parameterName)
      throws Exception {
    ExtensionParameter extensionParameter = new FieldWrapper(TestConnectionProvider.class
        .getField(parameterName), new DefaultExtensionsTypeLoaderFactory()
            .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    JavaParameterModelParser javaParameterModelParser =
        new JavaParameterModelParser(extensionParameter, Optional.empty(), forConnectionProvider("TestConnectionProvider"));
    return javaParameterModelParser.getOAuthParameterModelProperty();
  }

  private static class TestConnectionProvider implements ConnectionProvider<Object> {

    @Parameter
    public String parameter;

    @OAuthParameter(requestAlias = ALIAS,
        placement = HttpParameterPlacement.HEADERS)
    public String oAuthParameter;

    @org.mule.sdk.api.annotation.connectivity.oauth.OAuthParameter(requestAlias = ALIAS,
        placement = org.mule.sdk.api.runtime.parameter.HttpParameterPlacement.HEADERS)
    public String sdkOAuthParameter;

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }

}
