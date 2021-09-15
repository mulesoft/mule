/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.ANY;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.BOOLEAN;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.NUMBER;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.STRING;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.classloader.CompositeClassLoader;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's Runtime
 *
 * @since 4.0
 */
public final class MuleExtensionModelProvider {

  public static final String MULE_NAME = CORE_PREFIX;
  public static final String MULE_VERSION;
  public static final String MULESOFT_VENDOR = "MuleSoft, Inc.";

  public static final String MULE_TLS_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "tls");
  public static final String MULE_TLS_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";
  public static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault()
      .createTypeLoader(MuleExtensionModelProvider.class.getClassLoader());

  public static final BaseTypeBuilder BASE_TYPE_BUILDER = BaseTypeBuilder.create(JAVA);
  public static final MetadataType STRING_TYPE = loadPrimitive(STRING);
  public static final MetadataType INTEGER_TYPE = TYPE_LOADER.load(Integer.class);
  public static final MetadataType NUMBER_TYPE = loadPrimitive(NUMBER);
  public static final MetadataType BOOLEAN_TYPE = loadPrimitive(BOOLEAN);
  public static final MetadataType NULL_TYPE = BASE_TYPE_BUILDER.nullType().build();
  public static final MetadataType ANY_TYPE = loadPrimitive(ANY);
  public static final MetadataType VOID_TYPE = BASE_TYPE_BUILDER.voidType().build();
  public static final MetadataType OBJECT_STORE_TYPE = TYPE_LOADER.load(ObjectStore.class);

  static {
    // try {
    // final Properties buildProperties = new Properties();
    // buildProperties.load(MuleExtensionModelDeclarer.class.getResourceAsStream("/build.properties"));
    // MULE_VERSION = buildProperties.getProperty("muleVersion", "4.0.0");
    // } catch (IOException e) {
    // throw new MuleRuntimeException(e);
    // }
    MULE_VERSION = "4.5.0";
  }

  private static MetadataType loadPrimitive(String id) {
    return PRIMITIVE_TYPES.get(id);
  }

  private static ClassLoader getExtensionClassLoader() {
    ClassLoader containerClassLoader = MuleExtensionModelProvider.class.getClassLoader();
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    return containerClassLoader == contextClassLoader
        ? containerClassLoader
        : CompositeClassLoader.from(containerClassLoader, contextClassLoader);
  }

  private static ExtensionModel createExtensionModel(ExtensionDeclarer declarer) {
    ClassLoader containerClassLoader = MuleExtensionModelProvider.class.getClassLoader();
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    ClassLoader extensionClassLoader = containerClassLoader == contextClassLoader
        ? containerClassLoader
        : CompositeClassLoader.from(containerClassLoader, contextClassLoader);

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(extensionClassLoader);
      return new ExtensionModelFactory()
          .create(new DefaultExtensionLoadingContext(declarer, extensionClassLoader, new NullDslResolvingContext()));
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  // private static final LazyValue<ExtensionModel> EXTENSION_MODEL =
  // new LazyValue<>(() -> createExtensionModel(new MuleExtensionModelDeclarer().createExtensionModel()));
  //
  // private static final LazyValue<ExtensionModel> TLS_EXTENSION_MODEL =
  // new LazyValue<>(() -> createExtensionModel(new TlsExtensionModelDeclarer().createExtensionModel()));

  /**
   * @return the {@link ExtensionModel} definition for Mule's Runtime
   */
  public static ExtensionModel getExtensionModel() {
    // return EXTENSION_MODEL.get();
    return createExtensionModel(new MuleExtensionModelDeclarer().createExtensionModel());
  }

  /**
   * @return the {@link ExtensionModel} definition containing the namespace declaration for the tls module.
   */
  public static ExtensionModel getTlsExtensionModel() {
    // return TLS_EXTENSION_MODEL.get();
    return createExtensionModel(new TlsExtensionModelDeclarer().createExtensionModel());
  }
}
