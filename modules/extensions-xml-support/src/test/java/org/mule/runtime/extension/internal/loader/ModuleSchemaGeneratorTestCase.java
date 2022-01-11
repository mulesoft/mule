/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.runtime.module.extension.internal.capability.xml.schema.DefaultExtensionSchemaGenerator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private DefaultExtensionSchemaGenerator extensionSchemaFactory = new DefaultExtensionSchemaGenerator();

  @Parameterized.Parameters(name = "{index}: Validating xsd for {2}")
  public static Collection<Object[]> data() {
    final List<String> extensions = asList(
                                           "module-namespace-custom",
                                           "module-param-default-types",
                                           "module-param-custom-types",
                                           "module-param-role",
                                           "module-param-types",
                                           "module-properties-default-types",
                                           "module-properties-types",
                                           "module-single-op-with-property",
                                           "module-single-operation",
                                           "module-single-operation-camelized");

    return extensions.stream().map(moduleName -> {
      String modulePath = EXPECTED_FILES_DIR + moduleName + ".xml";

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();
      Map<String, Object> parameters = new HashMap<>();
      parameters.put(RESOURCE_XML, modulePath);
      // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
      parameters.put("COMPILATION_MODE", true);
      ExtensionModel extensionModel =
          new XmlExtensionModelLoader().loadExtensionModel(contextClassLoader, getDefault(getDependencies()), parameters);

      return new Object[] {extensionModel, moduleName + ".xsd"};
    }).collect(toList());
  }

  private static Set<ExtensionModel> getDependencies() {
    return singleton(getExtensionModel());
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
}
