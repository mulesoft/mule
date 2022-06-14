/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.sdk.api.annotation.semantics.security.TenantIdentifier;
import org.mule.test.values.extension.MyPojo;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class TestOAuthConnectionState {

  @RefName
  private String configName;

  @Parameter
  @Optional(defaultValue = "34.0")
  private Double apiVersion;

  /**
   * Tailors the login page to the user's device type.
   */
  @OAuthParameter
  private String display;

  /**
   * Avoid interacting with the user
   */
  @OAuthParameter
  @Optional(defaultValue = "false")
  private boolean immediate;

  @Parameter
  @Optional
  private ConnectionProperties connectionProperties;

  /**
   * Specifies how the authorization server prompts the user for reauthentication and reapproval
   */
  @OAuthParameter
  @Optional(defaultValue = "true")
  private boolean prompt;

  @OAuthCallbackValue(expression = "#[payload.instance_url]")
  private String instanceId;

  @OAuthCallbackValue(expression = "#[payload.id]")
  @TenantIdentifier
  private String userId;

  @ParameterGroup(name = "Connection details")
  private ConnectionDetails connectionDetails;

  @ParameterGroup(name = "Connection profile", showInDsl = true)
  private ConnectionProfile connectionProfile;

  @Parameter
  @Optional
  private ConnectionType oauthConnectionType;

  @Parameter
  @Optional
  private List<ConnectionProperties> someOauthConnectionProperties;

  @Parameter
  @Optional
  private List<Integer> someConnectionNumbers;

  @Parameter
  @Optional
  private Map<String, ConnectionProperties> someMapOfConnectionProperties;

  @Parameter
  @Optional
  private Integer securityLevel;

  @Parameter
  @Optional
  private TypedValue<Integer> typedSecurityLevel;

  @Parameter
  @Optional
  private Literal<String> literalSecurityDescription;

  @Parameter
  @Optional
  private ParameterResolver<String> resolverConnectionDisplayName;

  @Parameter
  @Optional
  private ZonedDateTime connectionTime;

  @Parameter
  @Optional
  private MyPojo externalPojo;

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<MyPojo>> stackedTypePojoParameter;

  private AuthorizationCodeState state;

  public Double getApiVersion() {
    return apiVersion;
  }

  public String getDisplay() {
    return display;
  }

  public boolean isImmediate() {
    return immediate;
  }

  public boolean isPrompt() {
    return prompt;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getUserId() {
    return userId;
  }

  public OAuthState getState() {
    return state;
  }

  public String getConfigName() {
    return configName;
  }

  public ConnectionProperties getConnectionProperties() {
    return connectionProperties;
  }

  public ConnectionDetails getConnectionDetails() {
    return connectionDetails;
  }

  public ConnectionProfile getConnectionProfile() {
    return connectionProfile;
  }

  public ConnectionType getOauthConnectionType() {
    return oauthConnectionType;
  }

  public Integer getSecurityLevel() {
    return securityLevel;
  }

  public List<ConnectionProperties> getSomeOauthConnectionProperties() {
    return someOauthConnectionProperties;
  }

  public List<Integer> getSomeConnectionNumbers() {
    return someConnectionNumbers;
  }

  public Map<String, ConnectionProperties> getSomeMapOfConnectionProperties() {
    return someMapOfConnectionProperties;
  }

  public TypedValue<Integer> getTypedSecurityLevel() {
    return typedSecurityLevel;
  }

  public Literal<String> getLiteralSecurityDescription() {
    return literalSecurityDescription;
  }

  public ParameterResolver<String> getResolverConnectionDisplayName() {
    return resolverConnectionDisplayName;
  }

  public ZonedDateTime getConnectionTime() {
    return connectionTime;
  }

  public MyPojo getExternalPojo() {
    return externalPojo;
  }

  public ParameterResolver<TypedValue<MyPojo>> getStackedTypePojoParameter() {
    return stackedTypePojoParameter;
  }
}
