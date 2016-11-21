/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.introspection.describer;

import static java.io.File.separator;
import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreAttributeOrder;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreComments;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreWhitespace;
import static org.custommonkey.xmlunit.XMLUnit.setNormalizeWhitespace;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber.XSD_SUFFIX;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaXmlResourceFactory;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
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

  private SchemaXmlResourceFactory schemaXmlResourceFactory;

  @Parameterized.Parameters(name = "{index}: Validating xsd for {2}")
  public static Collection<Object[]> data() {
    final Class classLoader = ModuleSchemaGeneratorTestCase.class;
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
      String moduleNamePrefix = "modules" + separator + moduleName;
      String modulePath = moduleNamePrefix + ".xml";

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();
      DescribingContext context = new DefaultDescribingContext(contextClassLoader);
      ExtensionFactory defaultExtensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), contextClassLoader);
      XmlBasedDescriber describer = new XmlBasedDescriber(modulePath);
      ExtensionModel extensionModel =
          withContextClassLoader(contextClassLoader,
                                 () -> defaultExtensionFactory.createFrom(describer.describe(context), context));

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
    schemaXmlResourceFactory = new SchemaXmlResourceFactory();
  }

  @Test
  public void generateXsd() throws Exception {
    Optional<GeneratedResource> generatedResource = schemaXmlResourceFactory.generateResource(extensionModel);

    String schema = new String(generatedResource.get().getContent());
    compareXML(expectedXSD, schema);
  }

  private void compareXML(String expected, String actual) throws Exception {
    setNormalizeWhitespace(true);
    setIgnoreWhitespace(true);
    setIgnoreComments(true);
    setIgnoreAttributeOrder(false);

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
