/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.UNAUTHORIZE_OPERATION_NAME;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.extension.internal.loader.util.JavaParserUtils.getExpressionSupport;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.util.DeclarationWalker;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.UnauthorizeOperationExecutor;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enriches OAuth enabled {@link ConnectionProviderDeclaration}s with further properties and behaviour which are specific to the
 * Java runtime implementation of the Extensions API
 *
 * @since 4.0
 */
public class JavaOAuthDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().enrich(extensionLoadingContext);
  }

  private class EnricherDelegate extends AbstractAnnotatedDeclarationEnricher {

    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private final MetadataType stringType = typeLoader.load(String.class);
    private final MetadataType voidType = typeLoader.load(void.class);

    @Override
    public void enrich(ExtensionLoadingContext extensionLoadingContext) {
      final ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();

      Set<Reference<ConnectionProviderDeclaration>> visitedProviders = new HashSet<>();
      Set<Reference<ConfigurationDeclaration>> oauthConfigs = new HashSet<>();
      Reference<Boolean> oauthGloballySupported = new Reference<>(false);
      Reference<Boolean> supportsAuthCode = new Reference<>(false);
      Reference<Boolean> supportsClientCredentials = new Reference<>(false);

      new DeclarationWalker() {

        @Override
        protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {

          declaration.getModelProperty(OAuthModelProperty.class).ifPresent(mp -> {
            mp.getGrantTypes().forEach(grantType -> grantType.accept(new OAuthGrantTypeVisitor() {

              @Override
              public void visit(AuthorizationCodeGrantType grantType) {
                supportsAuthCode.set(true);
              }

              @Override
              public void visit(ClientCredentialsGrantType grantType) {
                supportsClientCredentials.set(true);
              }

              @Override
              public void visit(PlatformManagedOAuthGrantType grantType) {
                // This grant type functions over completely synthetic connection providers
              }
            }));

            if (owner instanceof ExtensionDeclaration) {
              oauthGloballySupported.set(true);
            } else if (owner instanceof ConfigurationDeclaration) {
              oauthConfigs.add(new Reference<>((ConfigurationDeclaration) owner));
            }

            if (!visitedProviders.add(new Reference<>(declaration))) {
              return;
            }

          });
        }
      }.walk(extensionDeclaration);

      List<ConfigurationDeclaration> configs;
      if (oauthGloballySupported.get()) {
        configs = extensionDeclaration.getConfigurations();
      } else {
        configs = oauthConfigs.stream().map(Reference::get).collect(toList());
      }

      OperationDeclaration unauthorize = buildUnauthorizeOperation(supportsAuthCode.get());
      configs.forEach(c -> c.addOperation(unauthorize));
    }


    private void validateExpressionSupport(ConnectionProviderDeclaration provider,
                                           ParameterDeclaration parameter,
                                           Field field) {
      if (getExpressionSupport(field)
          .filter(expression -> expression == NOT_SUPPORTED)
          .isPresent()) {
        throw new IllegalConnectionProviderModelDefinitionException(format(
                                                                           "Parameter '%s' in Connection Provider '%s' is marked as supporting expressions. Expressions are not supported "
                                                                               + "in OAuth parameters",
                                                                           parameter.getName(),
                                                                           provider.getName()));

      }
    }

    private OperationDeclaration buildUnauthorizeOperation(boolean supportsAuthCode) {
      OperationDeclaration operation = new OperationDeclaration(UNAUTHORIZE_OPERATION_NAME);
      operation.setDescription("Deletes all the access token information of a given resource owner id so that it's impossible to "
          + "execute any operation for that user without doing the authorization dance again");
      operation.setBlocking(true);
      operation.setExecutionType(BLOCKING);
      operation.setOutput(toDeclaration(voidType));
      operation.setOutputAttributes(toDeclaration(voidType));
      operation.setRequiresConnection(false);
      operation.setSupportsStreaming(false);
      operation.setTransactional(false);
      operation
          .addModelProperty(new CompletableComponentExecutorModelProperty((model, params) -> new UnauthorizeOperationExecutor()));

      if (supportsAuthCode) {
        ParameterGroupDeclaration group = operation.getParameterGroup(DEFAULT_GROUP_NAME);
        group.showInDsl(false);

        ParameterDeclaration parameter = new ParameterDeclaration(RESOURCE_OWNER_ID_PARAMETER_NAME);
        parameter.setDescription("The id of the resource owner which access should be invalidated");
        parameter.setExpressionSupport(SUPPORTED);
        parameter.setLayoutModel(LayoutModel.builder().build());
        parameter.setRequired(false);
        parameter.setParameterRole(BEHAVIOUR);
        parameter.setType(stringType, false);

        group.addParameter(parameter);
      }

      return operation;
    }

    private OutputDeclaration toDeclaration(MetadataType type) {
      OutputDeclaration declaration = new OutputDeclaration();
      declaration.setType(type, false);

      return declaration;
    }
  }
}
