/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.display.PathModel.Location.EMBEDDED;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_PREFIX;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_TLS_NAMESPACE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.TLS_CONTEXT_FACTORY_TYPE;
import static org.mule.runtime.extension.api.ExtensionConstants.ALL_SUPPORTED_JAVA_VERSIONS;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OptionalParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

/**
 * An {@link ExtensionDeclarer} containing the namespace declaration for the tls module.
 *
 * @since 4.4
 */
class TlsExtensionModelDeclarer {

  ExtensionDeclarer createExtensionModel() {
    ExtensionDeclarer declarer = new ExtensionDeclarer()
        .named(TLS_PREFIX)
        .describedAs("Mule Runtime and Integration Platform: TLS components")
        .onVersion(MULE_VERSION)
        .fromVendor(MULESOFT_VENDOR)
        .supportingJavaVersions(ALL_SUPPORTED_JAVA_VERSIONS)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(TLS_PREFIX)
            .setNamespace(MULE_TLS_NAMESPACE)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-tls.xsd")
            .setSchemaLocation(MULE_TLS_SCHEMA_LOCATION)
            .build());

    declareExportedTypes(declarer);

    ConstructDeclarer context = declarer.withConstruct("context")
        .describedAs("""
            Reusable configuration element for TLS. A TLS context optionally defines a key store and a trust store.
            The key store contains the private and public keys of this server/client. The trust store contains
            certificates of the trusted servers/clients.""")
        .allowingTopLevelDefinition()
        .withStereotype(CONFIG);

    ParameterGroupDeclarer contextParams = context.onDefaultParameterGroup();
    stringParam(contextParams, "name",
                """
                    Reusable configuration element for TLS. A TLS context optionally defines a key store and a trust store.
                    The key store contains the private and public keys of this server/client.
                    The trust store contains certificates of the trusted servers/clients.""")
        .asComponentId();
    optionalStringParam(contextParams, "enabledProtocols", "A comma separated list of protocols enabled for this context.");
    optionalStringParam(contextParams, "enabledCipherSuites",
                        "A comma separated list of cipher suites enabled for this context.");

    declareTrustStore(context);
    declareKeyStore(context);
    declareRevocationCheck(context);

    return declarer;
  }

  private void declareExportedTypes(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarer.getDeclaration().addType((ObjectType) TLS_CONTEXT_FACTORY_TYPE);
  }

  private void declareRevocationCheck(ConstructDeclarer context) {
    NestedComponentDeclarer rc = context.withOptionalComponent("revocationCheck")
        .describedAs("Enable certificate revocation checking.");

    ParameterGroupDeclarer params = rc.onParameterGroup("standardRevocationCheck").withDslInlineRepresentation(true);
    booleanParam(params, "onlyEndEntities", "Only check the revocation status of end-entity certificates.");
    booleanParam(params, "preferCrls", "Prefer CRLs to OCSP. The default behavior is to prefer OCSP.");
    booleanParam(params, "noFallback", "Disable the fallback mechanism (the alternative algorithm, " +
        "for instance if CRLs are selected it would be OCSP)");
    booleanParam(params, "softFail", "Allow revocation check to succeed if the revocation status cannot be " +
        "determined because of network or server errors. This is a possible security risk.");

    params = rc.onParameterGroup("crlFile").withDslInlineRepresentation(true);
    configurePathParameter(optionalStringParam(params, "path", "The path to a CRL (Certificate Revocation List) " +
        "file to be used for this trust store. A certificate mentioned there will not be accepted for authentication."));

    params = rc.onParameterGroup("customOcspResponder").withDslInlineRepresentation(true);
    optionalStringParam(params, "url", "URL that identifies the location of the OCSP responder. " +
        "This is used instead of the corresponding field in the certificate extension.");
    optionalStringParam(params, "certAlias", "Alias of the certificate that signs the OCSP response, " +
        "instead of the corresponding CA. Must be present in the trust store.");
  }

  private void declareKeyStore(ConstructDeclarer context) {
    ParameterGroupDeclarer keyStore = context.onParameterGroup("keyStore").withDslInlineRepresentation(true);

    declarePathParameter(keyStore, "key store");
    declareStoreTypeParameter(keyStore);
    optionalStringParam(keyStore, "alias", "When the key store contains many private keys, this attribute indicates " +
        "the alias of the key that should be used. If not defined, the first key in the file will be used by default.");
    optionalStringParam(keyStore, "keyPassword", "The password used to protect the private key.")
        .withLayout(LayoutModel.builder().asPassword().build());
    optionalStringParam(keyStore, "password", "The password used to protect the key store.")
        .withLayout(LayoutModel.builder().asPassword().build());
    optionalStringParam(keyStore, "algorithm", "The algorithm used by the key store.");
  }

  private void declareTrustStore(ConstructDeclarer context) {
    ParameterGroupDeclarer trustStore = context.onParameterGroup("trustStore").withDslInlineRepresentation(true);
    declarePathParameter(trustStore, "trust store");
    optionalStringParam(trustStore, "password", "The password used to protect the trust store.")
        .withLayout(LayoutModel.builder().asPassword().build());
    declareStoreTypeParameter(trustStore);
    optionalStringParam(trustStore, "algorithm", "The algorithm used by the trust store.");
    booleanParam(trustStore, "insecure", "If true, no certificate validations will be performed.");
  }

  private void declareStoreTypeParameter(ParameterGroupDeclarer trustStore) {
    optionalStringParam(trustStore, "type", "The type of store used.")
        .withDisplayModel(DisplayModel.builder().example("jks, jceks, pkcs12 or other store type").build());
  }

  private void declarePathParameter(ParameterGroupDeclarer declarer, String resourceDescription) {
    configurePathParameter(optionalStringParam(declarer,
                                               "path",
                                               "The location (which will be resolved relative to the current classpath " +
                                                   "and file system, if possible) of the " + resourceDescription));
  }

  private void configurePathParameter(ParameterDeclarer declarer) {
    declarer.withDisplayModel(DisplayModel.builder()
        .path(new PathModel(FILE, false, EMBEDDED, new String[] {}))
        .build());
  }

  private ParameterDeclarer stringParam(ParameterGroupDeclarer declarer, String name, String description) {
    return declarer.withRequiredParameter(name)
        .describedAs(description)
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private OptionalParameterDeclarer booleanParam(ParameterGroupDeclarer declarer, String name, String description) {
    return optionalParam(declarer, name, description, BOOLEAN_TYPE).defaultingTo(false);
  }

  private OptionalParameterDeclarer optionalStringParam(ParameterGroupDeclarer declarer, String name, String description) {
    return optionalParam(declarer, name, description, STRING_TYPE);
  }

  private OptionalParameterDeclarer optionalParam(ParameterGroupDeclarer declarer,
                                                  String name,
                                                  String description,
                                                  MetadataType type) {
    return declarer.withOptionalParameter(name)
        .describedAs(description)
        .ofType(type)
        .withExpressionSupport(NOT_SUPPORTED);
  }
}
