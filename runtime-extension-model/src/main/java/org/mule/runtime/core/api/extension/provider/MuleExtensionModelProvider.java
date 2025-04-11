/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.ANY;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.BOOLEAN;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.NUMBER;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.STRING;
import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.DEFAULT_NAMESPACE_URI_MASK;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.lang.String.format;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurerFactory;

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
  public static final MetadataType LONG_TYPE = TYPE_LOADER.load(Long.class);
  public static final MetadataType NUMBER_TYPE = loadPrimitive(NUMBER);
  public static final MetadataType BOOLEAN_TYPE = loadPrimitive(BOOLEAN);
  public static final MetadataType NULL_TYPE = BASE_TYPE_BUILDER.nullType().build();
  public static final MetadataType ANY_TYPE = loadPrimitive(ANY);
  public static final MetadataType VOID_TYPE = BASE_TYPE_BUILDER.voidType().build();
  public static final MetadataType OBJECT_STORE_TYPE = TYPE_LOADER.load(ObjectStore.class);
  public static final MetadataType TLS_CONTEXT_FACTORY_TYPE = TYPE_LOADER.load(TlsContextFactory.class);

  private static ComponentMetadataConfigurerFactory configurerFactory = ComponentMetadataConfigurerFactory.getDefault();

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

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new MuleCoreExtensionModelLoader("mule")
      .loadExtensionModel(new MuleExtensionModelDeclarer(configurerFactory).createExtensionModel(),
                          loadingRequest()));

  private static final LazyValue<ExtensionModel> TLS_EXTENSION_MODEL =
      new LazyValue<>(() -> new MuleCoreExtensionModelLoader("mule-tls")
          .loadExtensionModel(new TlsExtensionModelDeclarer().createExtensionModel(),
                              loadingRequest()));

  private static final LazyValue<ExtensionModel> OPERATION_DSL_EXTENSION_MODEL =
      new LazyValue<>(() -> new MuleCoreExtensionModelLoader("mule-operationDsl")
          .loadExtensionModel(new MuleOperationExtensionModelDeclarer().declareExtensionModel(),
                              loadingRequest()));

  private static ExtensionModelLoadingRequest loadingRequest() {
    return builder(MuleExtensionModelProvider.class.getClassLoader(),
                   nullDslResolvingContext())
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

  public static void setConfigurerFactory(ComponentMetadataConfigurerFactory componentMetadataConfigurerFactory) {
    configurerFactory = componentMetadataConfigurerFactory;
  }

  private static final class MuleCoreExtensionModelLoader extends ExtensionModelLoader {

    private final String id;

    public MuleCoreExtensionModelLoader(String id) {
      this.id = id;
    }

    @Override
    protected void declareExtension(ExtensionLoadingContext context) {
      // nothing to do
    }

    @Override
    public String getId() {
      return id;
    }
  }

}
