/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.chains.api.extension;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.extension.api.ExtensionConstants.ALL_SUPPORTED_JAVA_VERSIONS;
import static org.mule.tests.chains.api.config.TestProcessorChainsNamespaceInfoProvider.TEST_PROCESSOR_CHAINS_NAMESPACE;

import static java.lang.String.format;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.version.MuleMinorVersion;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

public class TestProcessorChainsExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  public static final String EXTENSION_NAME = "Test Processor chains";
  public static final String EXTENSION_DESCRIPTION = "Spring Module Plugin";
  public static final String VENDOR = "Mulesoft";
  public static final String VERSION = "1.4.0-SNAPSHOT";
  public static final MuleMinorVersion MIN_MULE_VERSION = new MuleMinorVersion("4.4");
  public static final String XSD_FILE_NAME = "mule-test-processor-chains.xsd";
  private static final String UNESCAPED_LOCATION_PREFIX = "http://";
  private static final String SCHEMA_LOCATION = "www.mulesoft.org/schema/mule/test-processor-chains";
  private static final String SCHEMA_VERSION = "current";

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    XmlDslModel xmlDslModel = XmlDslModel.builder()
        .setPrefix(TEST_PROCESSOR_CHAINS_NAMESPACE)
        .setXsdFileName(XSD_FILE_NAME)
        .setSchemaVersion(VERSION)
        .setSchemaLocation(format("%s/%s/%s", UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, SCHEMA_VERSION, XSD_FILE_NAME))
        .setNamespace(UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION)
        .build();

    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    extensionDeclarer.named(EXTENSION_NAME)
        .describedAs(EXTENSION_DESCRIPTION)
        .fromVendor(VENDOR)
        .onVersion(VERSION)
        .supportingJavaVersions(ALL_SUPPORTED_JAVA_VERSIONS)
        .withCategory(COMMUNITY)
        .withXmlDsl(xmlDslModel);

    final ConstructDeclarer compositeProcessorChainRouter = extensionDeclarer.withConstruct("compositeProcessorChainRouter");
    compositeProcessorChainRouter
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(typeLoader.load(String.class))
        .asComponentId();

    final ConstructDeclarer processorChainRouter = extensionDeclarer.withConstruct("processorChainRouter");
    processorChainRouter
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(typeLoader.load(String.class))
        .asComponentId();

    compositeProcessorChainRouter.withChain("chain").withMinOccurs(1);

  }

}
