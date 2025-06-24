/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.policy.api.extension;

import static org.mule.functional.policy.api.TestPolicyXmlNamespaceInfoProvider.TEST_POLICY_NAMESPACE;
import static org.mule.functional.policy.api.TestPolicyXmlNamespaceInfoProvider.TEST_POLICY_PREFIX;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.ANY_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.ExtensionConstants.ALL_SUPPORTED_JAVA_VERSIONS;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoErrorMapping;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.core.internal.extension.CustomLocationPartModelProperty;

/**
 * An {@link ExtensionDeclarer} for test Policy components
 *
 * @since 4.4
 */
class TestPolicyExtensionModelDeclarer {

  public ExtensionDeclarer createExtensionModel() {
    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named(TEST_POLICY_PREFIX)
        .describedAs("Mule Runtime and Integration Platform: test Policy components")
        .onVersion(MULE_VERSION)
        .fromVendor("MuleSoft, Inc.")
        .withCategory(SELECT)
        .supportingJavaVersions(ALL_SUPPORTED_JAVA_VERSIONS)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(TEST_POLICY_PREFIX)
            .setNamespace(TEST_POLICY_NAMESPACE)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-test-policy.xsd")
            .setSchemaLocation(buildSchemaLocation(TEST_POLICY_PREFIX, TEST_POLICY_NAMESPACE))
            .build());

    declareProxy(extensionDeclarer);
    declareExecuteNext(extensionDeclarer);
    declareCustomProcessor(extensionDeclarer);

    return extensionDeclarer;
  }

  private void declareProxy(ExtensionDeclarer extensionDeclarer) {
    final ConstructDeclarer proxyDeclarer = extensionDeclarer.withConstruct("proxy")
        .allowingTopLevelDefinition();

    proxyDeclarer
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .describedAs("The name used to identify this policy.")
        .asComponentId()
        .ofType(STRING_TYPE);

    proxyDeclarer
        .withRoute("source")
        .withMinOccurs(0)
        .withMaxOccurs(1)
        .withModelProperty(new CustomLocationPartModelProperty("source", false))
        .withChain();

    proxyDeclarer
        .withRoute("operation")
        .withMinOccurs(1)
        .withMaxOccurs(1)
        .withModelProperty(new CustomLocationPartModelProperty("operation", false))
        .withChain();
  }

  private void declareExecuteNext(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer executeNext = extensionDeclarer
        .withOperation("executeNext");
    withNoErrorMapping(executeNext);

    // By this operation alone we cannot determine what its output will be, it will depend on the context on which this operation
    // is located.
    executeNext.withOutput().ofType(ANY_TYPE);
    executeNext.withOutputAttributes().ofType(ANY_TYPE);
  }

  private void declareCustomProcessor(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer executeNext = extensionDeclarer
        .withOperation("customProcessor");
    withNoErrorMapping(executeNext);

    executeNext.withOutput().ofType(ANY_TYPE);
    executeNext.withOutputAttributes().ofType(ANY_TYPE);

    executeNext.onDefaultParameterGroup().withRequiredParameter("class").ofType(STRING_TYPE);
  }
}
