/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.runtime.module.extension.internal.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.COMPILATION_MODE;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public abstract class ParameterizedExtensionModelTestCase extends AbstractMuleTestCase {

  protected static Map<String, ExtensionModel> EXTENSION_MODELS = new HashMap<>();

  protected static final ExtensionModelLoader JAVA_LOADER = new DefaultJavaExtensionModelLoader();
  protected static final ExtensionModelLoader SOAP_LOADER = new SoapExtensionModelLoader();

  @Parameterized.Parameter
  public ExtensionModel extensionUnderTest;

  @AfterClass
  public static void cleanUp() {
    EXTENSION_MODELS = new HashMap<>();
  }

  protected static Collection<Object[]> createExtensionModels(List<? extends ExtensionUnitTest> extensions) {
    BiFunction<Class<?>, ExtensionModelLoader, ExtensionModel> createExtensionModel = (extension, loader) -> {
      ExtensionModel model = loadExtension(extension, loader);

      if (EXTENSION_MODELS.put(model.getName(), model) != null) {
        throw new IllegalArgumentException(format("Extension names must be unique. Name [%s] for extension [%s] was already used",
                                                  model.getName(), extension.getName()));
      }

      return model;
    };

    return extensions.stream()
        .map(e -> e.toTestParams(createExtensionModel))
        .collect(toList());
  }

  protected static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader) {
    Map<String, Object> params = of(TYPE_PROPERTY_NAME, clazz.getName(),
                                    VERSION, getProductVersion(),
                                    // TODO MULE-14517: This workaround should be replaced for a better and more complete
                                    // mechanism
                                    COMPILATION_MODE, true);

    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(new LinkedHashSet<>(EXTENSION_MODELS.values()));

    final String basePackage = clazz.getPackage().toString();
    final ClassLoader pluginClassLoader = new ClassLoader(clazz.getClassLoader()) {

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(basePackage)) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass().getResourceAsStream("/" + name.replaceAll("\\.", "/") + ".class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name, resolve);
        }
      }
    };

    return loader.loadExtensionModel(pluginClassLoader, dslResolvingContext, params);
  }

  public static class ExtensionUnitTest {

    final ExtensionModelLoader loader;
    final Class<?> extensionClass;

    protected ExtensionUnitTest(ExtensionModelLoader loader, Class<?> extensionClass) {
      this.loader = loader;
      this.extensionClass = extensionClass;
    }

    static ExtensionUnitTest newTestUnit(ExtensionModelLoader loader, Class<?> extensionClass) {
      return new ExtensionUnitTest(loader, extensionClass);
    }

    ExtensionModelLoader getLoader() {
      return loader;
    }

    Class<?> getExtensionClass() {
      return extensionClass;
    }

    public final Object[] toTestParams(BiFunction<Class<?>, ExtensionModelLoader, ExtensionModel> createExtensionModel) {
      final ExtensionModel extensionModel = createExtensionModel.apply(getExtensionClass(), getLoader());
      return buildTestParams(extensionModel);
    }

    protected Object[] buildTestParams(ExtensionModel extensionModel) {
      return new Object[] {extensionModel};
    }
  }
}
