/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.schema;

import static java.util.stream.Collectors.toList;
import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.loader.ModuleExtensionStore;
import org.mule.runtime.config.spring.dsl.model.extension.schema.ModuleSchemaGenerator;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ModuleSchemaGeneratorTestCase extends AbstractMuleTestCase {

  private ModuleSchemaGenerator schemaGenerator;

  @Parameterized.Parameter
  public ModuleExtension moduleExtension;

  @Parameterized.Parameter(1)
  public String expectedXSD;

  @Parameterized.Parameters(name = "{index}: Validating xsd for {0}")
  public static Collection<Object[]> data() {
    final Class classLoader = ModuleSchemaGeneratorTestCase.class;
    final ModuleExtensionStore moduleExtensionStore = new ModuleExtensionStore();
    final List<String> extensions = new ArrayList<String>() {

      {
        add("module-param-default-types");
        add("module-param-types");
        add("module-properties-default-types");
        add("module-properties-types");
        add("module-single-operation");
        add("module-single-op-with-property");
      }
    };

    Function<String, Object[]> stringFunction = moduleName -> {
      final String namespaceTestPrefix = "http://www.mulesoft.org/schema/mule/";
      final String namespace = namespaceTestPrefix + moduleName;
      Optional<ModuleExtension> module = moduleExtensionStore.lookupByNamespace(namespace);
      if (module.isPresent()) {
        try {
          return new Object[] {module.get(),
              IOUtils.getResourceAsString("modules/" + moduleName + ".xsd", classLoader)
          };
        } catch (IOException e) {
          throw new IllegalArgumentException(String.format("Couldn't load .xsd for the [%s] module", moduleName));
        }
      }
      throw new IllegalArgumentException(String.format("Couldn't find module for the [%s] namespace", namespace));
    };
    return extensions.stream().map(stringFunction).collect(toList());
  }

  @Before
  public void setUp() throws IOException {
    this.schemaGenerator = new ModuleSchemaGenerator();
  }

  @Test
  public void generateXsd() throws Exception {
    String schema = IOUtils.toString(schemaGenerator.getSchema(moduleExtension));
    compareXML(expectedXSD, schema);
  }

  private void compareXML(String expected, String actual) throws Exception {
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    XMLUnit.setIgnoreAttributeOrder(false);

    Diff diff = XMLUnit.compareXML(expected, actual);
    if (!(diff.similar() && diff.identical())) {
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
