/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.config;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CRL_FILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CUSTOM_OCSP_RESPONDER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_KEY_STORE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_STANDARD_REVOCATION_CHECK_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_TRUST_STORE_ELEMENT_IDENTIFIER;

import org.mule.runtime.core.privileged.security.RevocationCheck;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.runtime.module.tls.internal.revocation.CrlFile;
import org.mule.runtime.module.tls.internal.revocation.CustomOcspResponder;
import org.mule.runtime.module.tls.internal.revocation.StandardRevocationCheck;

import java.util.LinkedList;
import java.util.List;

/**
 * A {@link ComponentBuildingDefinitionProvider} for TLS related definitions
 *
 * @since 4.0
 */
public class TlsComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  public static final String TLS_NAMESPACE = "tls";

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(TLS_NAMESPACE);

  @Override
  public void init() {

  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    ComponentBuildingDefinition.Builder baseStore = baseDefinition
        .withSetterParameterDefinition("path", fromSimpleParameter("path").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withSetterParameterDefinition("type", fromSimpleParameter("type").build())
        .withSetterParameterDefinition("algorithm", fromSimpleParameter("algorithm").build());

    componentBuildingDefinitions
        .add(baseStore.withIdentifier(TLS_KEY_STORE_ELEMENT_IDENTIFIER).withTypeDefinition(fromType(KeyStoreConfig.class))
            .withSetterParameterDefinition("alias", fromSimpleParameter("alias").build())
            .withSetterParameterDefinition("keyPassword", fromSimpleParameter("keyPassword").build()).build());

    componentBuildingDefinitions
        .add(baseStore.withIdentifier(TLS_TRUST_STORE_ELEMENT_IDENTIFIER).withTypeDefinition(fromType(TrustStoreConfig.class))
            .withSetterParameterDefinition("insecure", fromSimpleParameter("insecure").build()).build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TLS_STANDARD_REVOCATION_CHECK_ELEMENT_IDENTIFIER)
            .withTypeDefinition(fromType(StandardRevocationCheck.class))
            .withSetterParameterDefinition("onlyEndEntities", fromSimpleParameter("onlyEndEntities").build())
            .withSetterParameterDefinition("preferCrls", fromSimpleParameter("preferCrls").build())
            .withSetterParameterDefinition("noFallback", fromSimpleParameter("noFallback").build())
            .withSetterParameterDefinition("softFail", fromSimpleParameter("softFail").build())
            .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TLS_CUSTOM_OCSP_RESPONDER_ELEMENT_IDENTIFIER)
            .withTypeDefinition(fromType(CustomOcspResponder.class))
            .withSetterParameterDefinition("url", fromSimpleParameter("url").build())
            .withSetterParameterDefinition("certAlias", fromSimpleParameter("certAlias").build())
            .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TLS_CRL_FILE_ELEMENT_IDENTIFIER).withTypeDefinition(fromType(CrlFile.class))
            .withSetterParameterDefinition("path", fromSimpleParameter("path").build())
            .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TLS_CONTEXT_ELEMENT_IDENTIFIER)
            .withTypeDefinition(fromType(DefaultTlsContextFactory.class))
            .withObjectFactoryType(DefaultTlsContextFactoryObjectFactory.class)
            .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
            .withSetterParameterDefinition("enabledProtocols", fromSimpleParameter("enabledProtocols").build())
            .withSetterParameterDefinition("enabledCipherSuites", fromSimpleParameter("enabledCipherSuites").build())
            .withSetterParameterDefinition("keyStore", fromChildConfiguration(KeyStoreConfig.class).build())
            .withSetterParameterDefinition("trustStore", fromChildConfiguration(TrustStoreConfig.class).build())
            .withSetterParameterDefinition("revocationCheck", fromChildConfiguration(RevocationCheck.class)
                .withWrapperIdentifier(TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER).build())
            .build());

    return componentBuildingDefinitions;
  }
}
