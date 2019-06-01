/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFields;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.ClientCredentials;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Validates that OAuth enabled {@link ConnectionProvider}s are well formed
 *
 * @since 4.0
 */
public class OAuthConnectionProviderModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        Class<?> implementingType = model.getModelProperty(ImplementingTypeModelProperty.class)
            .map(ImplementingTypeModelProperty::getType)
            .orElse(null);

        if (implementingType == null) {
          return;
        }

        AuthorizationCode authCode = implementingType.getAnnotation(AuthorizationCode.class);
        ClientCredentials clientCredentials = implementingType.getAnnotation(ClientCredentials.class);

        if (authCode != null && clientCredentials != null) {
          throw new IllegalConnectionProviderModelDefinitionException(format(
                                                                             "Connection Provider of class '%s' is attempting to support both authorization code and client credentials "
                                                                                 + "grant types. Each connection provider can only support one grant type at a time.",
                                                                             implementingType));
        }

        if (authCode != null) {
          validateStateField(implementingType, AuthorizationCodeState.class, "authorization code");
        }

        if (clientCredentials != null) {
          validateStateField(implementingType, ClientCredentialsState.class, "client credentials");
        }
      }
    }.walk(model);
  }

  private void validateStateField(Class<?> implementingType, Class<?> stateFieldType, String grantType) {
    List<Field> stateFields = getFields(implementingType).stream()
        .filter(f -> f.getType().equals(stateFieldType))
        .collect(toList());

    if (stateFields.size() != 1) {
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection Provider of class '%s' uses OAuth2 %s grant type and thus should contain "
                                                                      + "one (and only one) field of type %s. %d were found",
                                                                         implementingType,
                                                                         grantType,
                                                                         AuthorizationCodeState.class.getName(),
                                                                         stateFields.size()));
    }
  }
}
