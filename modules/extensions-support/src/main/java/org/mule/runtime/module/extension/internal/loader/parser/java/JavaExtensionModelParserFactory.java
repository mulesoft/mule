/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;

/**
 * {@link ExtensionModelParserFactory} implementation for Java based extensions
 *
 * @since 4.5.0
 */
public class JavaExtensionModelParserFactory implements ExtensionModelParserFactory {

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    return new JavaExtensionModelParser(getExtensionElement(context), context);
  }

  /**
   * Extracts the {@link ExtensionElement} from the given {@code context}
   *
   * @param context the loading context
   * @return an {@link ExtensionElement}
   */
  public static ExtensionElement getExtensionElement(ExtensionLoadingContext context) {
    return context.getExtensionDeclarer().getDeclaration().getModelProperty(ExtensionTypeDescriptorModelProperty.class)
        .map(p -> {
          Type type = p.getType();
          if (type instanceof ExtensionElement) {
            return (ExtensionElement) type;
          } else if (type instanceof TypeWrapper) {
            return new ExtensionTypeWrapper(type.getDeclaringClass().get(), getDefault().createTypeLoader());
          } else {
            throw new IllegalArgumentException("Unsupported declaration class type: " + type.getClass().getName());
          }
        })
        .orElseGet(() -> context.getParameter(EXTENSION_TYPE)
            .map(type -> toExtensionElement(type))
            .orElseGet(() -> {
              String type = (String) context.getParameter(TYPE_PROPERTY_NAME).get();
              try {
                ClassLoader extensionClassLoader = context.getExtensionClassLoader();
                return new ExtensionTypeWrapper<>(loadClass(type, extensionClassLoader),
                                                  context.getTypeLoader());
              } catch (ClassNotFoundException e) {
                throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
              }
            }));
  }

  private static ExtensionElement toExtensionElement(Object type) {
    if (type instanceof ExtensionElement) {
      return (ExtensionElement) type;
    } else if (type instanceof Class) {
      return new ExtensionTypeWrapper((Class) type, getDefault().createTypeLoader());
    } else {
      throw new IllegalArgumentException("Unsupported extension type: " + type);
    }
  }
}
