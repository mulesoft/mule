/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;
import org.mule.test.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests to ensure XSD generation coming from an XML using the {@link ExtensionModel} mechanism.
 *
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class ModuleSchemaGeneratorTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  private static final String EXPECTED_FILES_DIR = "modules/schema/";
  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionSchemas.updateExpectedFilesOnError");
  private static final ExtensionModel PET_STORE_EXTENSION_MODEL = MuleExtensionUtils.loadExtension(PetStoreConnector.class);

  private final ExtensionSchemaGenerator extensionSchemaFactory =
      ServiceLoader.load(ExtensionSchemaGenerator.class).iterator().next();

  @Parameterized.Parameters(name = "{index}: Validating xsd for {1}")
  public static Collection<Object[]> data() {
    return asList(
                  new Object[] {new XmlExtensionModelLoader(), "module-namespace-custom",
                      "module-namespace-custom.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-param-default-types",
                      "module-param-default-types.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-param-custom-types",
                      "module-param-custom-types.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-param-role",
                      "module-param-role.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-param-types",
                      "module-param-types.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-properties-default-types",
                      "module-properties-default-types.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-properties-types",
                      "module-properties-types.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-single-op-with-property",
                      "module-single-op-with-property.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-single-operation",
                      "module-single-operation.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-single-operation-camelized",
                      "module-single-operation-camelized.xsd",
                      null, emptyList()},
                  new Object[] {new XmlExtensionModelLoader(), "module-tls-config",
                      "module-tls-config.xsd",
                      null, emptyList()});
  }

  @Parameterized.Parameter(1)
  public String extensionModulePath;

  @Override
  public void createDslResolvingContext() throws IOException {
    dslResolvingContext = getDefault(getDependencies());
  }

  private static Set<ExtensionModel> getDependencies() {
    Set<ExtensionModel> dependencies = new HashSet<>();
    dependencies.add(getExtensionModel());
    dependencies.add(PET_STORE_EXTENSION_MODEL);
    return dependencies;
  }

  @Override
  protected boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Override
  protected String getExpectedFilesDir() {
    return EXPECTED_FILES_DIR;
  }

  @Override
  protected String doGenerate(ExtensionModel extensionUnderTest) {
    return extensionSchemaFactory.generate(extensionUnderTest, getDefault(emptySet()));
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception {
    compareXML(expectedContent, actualContent);
  }

  @Override
  protected ExtensionModel doLoadExtension() {
    ClassLoader contextClassLoader = currentThread().getContextClassLoader();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, EXPECTED_FILES_DIR + extensionModulePath + ".xml");
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    parameters.put("COMPILATION_MODE", true);

    return new XmlExtensionModelLoader().loadExtensionModel(contextClassLoader, getDefault(getDependencies()), parameters);
  }

}
