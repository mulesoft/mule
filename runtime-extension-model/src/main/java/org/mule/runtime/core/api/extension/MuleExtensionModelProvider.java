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
import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_EXTENSION_VALIDATION_PROPERTY_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's Runtime
 *
 * @since 4.0
 */
public final class MuleExtensionModelProvider {

  private static final MuleVersion PARSED_MULE_VERSION;

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
    try {
      final Properties buildProperties = new Properties();
      buildProperties.load(MuleExtensionModelDeclarer.class.getResourceAsStream("/build.properties"));
      MULE_VERSION = buildProperties.getProperty("muleVersion", "4.0.0");
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }

    PARSED_MULE_VERSION = new MuleVersion(MULE_VERSION);
  }

  private static MetadataType loadPrimitive(String id) {
    return PRIMITIVE_TYPES.get(id);
  }

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(contextFor(new MuleExtensionModelDeclarer().createExtensionModel())));

  private static final LazyValue<ExtensionModel> TLS_EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(contextFor(new TlsExtensionModelDeclarer().createExtensionModel())));

  private static final LazyValue<ExtensionModel> OPERATION_DSL_EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(contextFor(new MuleOperationExtensionModelDeclarer().declareExtensionModel())));

  private static ExtensionLoadingContext contextFor(ExtensionDeclarer declarer) {
    return new DefaultExtensionLoadingContext(declarer, loadingRequest());
  }

  private static ExtensionModelLoadingRequest loadingRequest() {
    return ExtensionModelLoadingRequest.builder(MuleExtensionModelProvider.class.getClassLoader(), new NullDslResolvingContext())
        .build();
  }

  /**
   * @return the {@link ExtensionModel} definition for Mule's Runtime
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }

  /**
   * @return the {@link ExtensionModel} definition containing the namespace declaration for the tls module.
   */
  public static ExtensionModel getTlsExtensionModel() {
    return TLS_EXTENSION_MODEL.get();
  }

  /**
   * @return the {@link ExtensionModel} definition containing the namespace declaration for the operation declaration DSL
   * @since 4.5.0
   */
  public static ExtensionModel getOperationDslExtensionModel() {
    return OPERATION_DSL_EXTENSION_MODEL.get();
  }

  /**
   * @return the parsed {@link MuleVersion} from the build properties.
   */
  public static MuleVersion getMuleVersion() {
    return PARSED_MULE_VERSION;
  }
}
