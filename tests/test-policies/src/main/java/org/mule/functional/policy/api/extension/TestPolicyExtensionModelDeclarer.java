/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.policy.api.extension;

import static org.mule.functional.policy.api.TestPolicyXmlNamespaceInfoProvider.TEST_POLICY_NAMESPACE;
import static org.mule.functional.policy.api.TestPolicyXmlNamespaceInfoProvider.TEST_POLICY_PREFIX;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.ANY_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CustomLocationPartModelProperty;

/**
 * An {@link ExtensionDeclarer} for test Policy components
 *
 * @since 4.4
 */
class TestPolicyExtensionModelDeclarer {

  public ExtensionDeclarer createExtensionModel() {
    final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JavaTypeLoader.JAVA);

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named(TEST_POLICY_PREFIX)
        .describedAs("Mule Runtime and Integration Platform: test Policy components")
        .onVersion(MULE_VERSION)
        .fromVendor("MuleSoft, Inc.")
        .withCategory(SELECT)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(TEST_POLICY_PREFIX)
            .setNamespace(TEST_POLICY_NAMESPACE)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-test-policy.xsd")
            .setSchemaLocation(buildSchemaLocation(TEST_POLICY_PREFIX, TEST_POLICY_NAMESPACE))
            .build());

    declareProxy(typeBuilder, extensionDeclarer);
    declareExecuteNext(extensionDeclarer);

    return extensionDeclarer;
  }

  private void declareProxy(final BaseTypeBuilder typeBuilder, ExtensionDeclarer extensionDeclarer) {
    final ConstructDeclarer proxyDeclarer = extensionDeclarer.withConstruct("proxy")
        .allowingTopLevelDefinition();

    proxyDeclarer
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .describedAs("The name used to identify this policy.")
        .asComponentId()
        .ofType(STRING_TYPE);

    proxyDeclarer
        .withChain("source")
        .setRequired(false)
        .withModelProperty(new CustomLocationPartModelProperty("source", false));

    proxyDeclarer
        .withChain("operation")
        .withModelProperty(new CustomLocationPartModelProperty("operation", false));
  }

  private void declareExecuteNext(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer executeNext = extensionDeclarer
        .withOperation("executeNext")
        .withModelProperty(new NoErrorMappingModelProperty());

    // By this operation alone we cannot determine what its output will be, it will depend on the context on which this operation
    // is located.
    executeNext.withOutput().ofType(ANY_TYPE);
    executeNext.withOutputAttributes().ofType(ANY_TYPE);
  }

}
