/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;

import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;

public class JavaExtensionModelParserFactory implements ExtensionModelParserFactory {

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    return new JavaExtensionModelParser(getExtensionElement(context), context);
  }

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
        .orElseGet(() -> context.<ExtensionElement>getParameter(AbstractJavaExtensionModelLoader.EXTENSION_TYPE).orElseGet(() -> {
          String type = (String) context.getParameter(TYPE_PROPERTY_NAME).get();
          try {
            ClassLoader extensionClassLoader = context.getExtensionClassLoader();
            return new ExtensionTypeWrapper<>(loadClass(type, extensionClassLoader),
                                              new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionClassLoader));
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
          }
        }));
  }
}
