/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.enricher.XmlModelEnricher;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalInnerPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.ListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.MapConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.StringListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.TestConnector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.vegan.extension.VeganExtension;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class SchemaGeneratorTestCase extends AbstractMuleTestCase {

  static final Map<String, RuntimeExtensionModel> extensionModels = new HashMap<>();

  @Parameterized.Parameter(0)
  public ExtensionModel extensionUnderTest;

  @Parameterized.Parameter(1)
  public String expectedXSD;

  private SchemaGenerator generator;
  private String expectedSchema;


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    final ClassLoader classLoader = SchemaGeneratorTestCase.class.getClassLoader();
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader)).thenReturn(asList(new XmlModelEnricher()));

    final ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), classLoader);

    final Map<Class<?>, String> extensions = new LinkedHashMap<Class<?>, String>() {

      {
        put(HeisenbergExtension.class, "heisenberg.xsd");
        put(TestConnector.class, "basic.xsd");
        put(GlobalPojoConnector.class, "global-pojo.xsd");
        put(GlobalInnerPojoConnector.class, "global-inner-pojo.xsd");
        put(MapConnector.class, "map.xsd");
        put(ListConnector.class, "list.xsd");
        put(StringListConnector.class, "string-list.xsd");
        put(VeganExtension.class, "vegan.xsd");
        put(SubTypesMappingConnector.class, "subtypes.xsd");
        put(PetStoreConnector.class, "petstore.xsd");
        put(MetadataExtension.class, "metadata.xsd");
      }
    };

    Function<Class<?>, ExtensionModel> createExtensionModel = extension -> {
      ExtensionDeclarer declarer = new AnnotationsBasedDescriber(extension, new StaticVersionResolver(getProductVersion()))
          .describe(new DefaultDescribingContext(extension.getClassLoader()));
      RuntimeExtensionModel model = extensionFactory.createFrom(declarer, new DefaultDescribingContext(declarer, classLoader));

      if (extensionModels.put(model.getName(), model) != null) {
        throw new IllegalArgumentException(format("Extension names must be unique. Name [%s] for extension [%s] was already used",
                                                  model.getName(), extension.getName()));
      }

      return model;
    };

    return extensions.entrySet().stream()
        .map(e -> new Object[] {createExtensionModel.apply(e.getKey()), e.getValue()})
        .collect(toList());
  }

  @Before
  public void setup() throws IOException {
    generator = new SchemaGenerator();
    expectedSchema = IOUtils.getResourceAsString("schemas/" + expectedXSD, getClass());
  }

  @Test
  public void generate() throws Exception {
    XmlModelProperty capability = extensionUnderTest.getModelProperty(XmlModelProperty.class).get();

    String schema = generator.generate(extensionUnderTest, capability, new SchemaTestDslContext());
    compareXML(expectedSchema, schema);
  }

  private static class SchemaTestDslContext implements DslResolvingContext {

    @Override
    public Optional<ExtensionModel> getExtension(String name) {
      return Optional.ofNullable(extensionModels.get(name));
    }
  }

}
