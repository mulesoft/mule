/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.io.File.separator;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreAttributeOrder;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreComments;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreWhitespace;
import static org.custommonkey.xmlunit.XMLUnit.setNormalizeWhitespace;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.XSD_SUFFIX;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.extension.internal.capability.xml.schema.DefaultExtensionSchemaGenerator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests to ensure XSD generation coming from an XML using the {@link ExtensionModel} mechanism.
 *
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class ModuleSchemaGeneratorTestCase extends AbstractMuleTestCase {

  @Parameterized.Parameter
  public ExtensionModel extensionModel;

  @Parameterized.Parameter(1)
  public String expectedXSD;

  @Parameterized.Parameter(2)
  public String extensionName;

  private DefaultExtensionSchemaGenerator extensionSchemaFactory;

  @Parameterized.Parameters(name = "{index}: Validating xsd for {2}")
  public static Collection<Object[]> data() {
    final Class classLoader = ModuleSchemaGeneratorTestCase.class;
    final List<String> extensions = new ArrayList<String>() {

      {
        add("module-namespace-custom");
        add("module-param-default-types");
        add("module-param-custom-types");
        add("module-param-role");
        add("module-param-types");
        add("module-properties-default-types");
        add("module-properties-types");
        add("module-single-op-with-property");
        add("module-single-operation");
      }
    };

    Function<String, Object[]> stringFunction = moduleName -> {
      String moduleNamePrefix = "modules" + separator + "schema" + separator + moduleName;
      String modulePath = moduleNamePrefix + ".xml";

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();
      Map<String, Object> parameters = new HashMap<>();
      parameters.put(RESOURCE_XML, modulePath);
      ExtensionModel extensionModel =
          new XmlExtensionModelLoader().loadExtensionModel(contextClassLoader, getDefault(emptySet()), parameters);

      try {
        return new Object[] {extensionModel,
            IOUtils.getResourceAsString(moduleNamePrefix + XSD_SUFFIX, classLoader), extensionModel.getName()
        };
      } catch (IOException e) {
        throw new IllegalArgumentException(String.format("Couldn't load .xsd for the [%s] module", moduleName));
      }
    };
    return extensions.stream().map(stringFunction).collect(toList());
  }

  @Before
  public void setUp() throws IOException {
    extensionSchemaFactory = new DefaultExtensionSchemaGenerator();
  }

  @Test
  public void generateXsd() throws Exception {
    String generatedSchema = extensionSchemaFactory.generate(extensionModel, getDefault(emptySet()));
    compareXML(expectedXSD, generatedSchema);
  }

  private void compareXML(String expected, String actual) throws Exception {
    setNormalizeWhitespace(true);
    setIgnoreWhitespace(true);
    setIgnoreComments(true);
    setIgnoreAttributeOrder(false);

    Diff diff = XMLUnit.compareXML(expected, actual);
    if (!(diff.similar() && diff.identical())) {
      System.out.println(actual);
      DetailedDiff detDiff = new DetailedDiff(diff);
      @SuppressWarnings("rawtypes")
      List differences = detDiff.getAllDifferences();
      StringBuilder diffLines = new StringBuilder();
      for (Object object : differences) {
        Difference difference = (Difference) object;
        diffLines.append(difference.toString() + '\n');
      }
      throw new IllegalArgumentException("Actual XML differs from expected: \n" + diffLines.toString());
    }
  }
}
