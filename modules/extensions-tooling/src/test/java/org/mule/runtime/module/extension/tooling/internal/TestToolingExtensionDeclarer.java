/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal;

import static java.lang.Thread.currentThread;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.tooling.internal.extension.TestConnectionProvider.COMPLEX_PARAMETER_GROUP_NAME;
import static org.springframework.util.ReflectionUtils.findField;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.WithAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.tooling.internal.extension.AnnoyingPojo;
import org.mule.runtime.module.extension.tooling.internal.extension.ComplexParameterGroup;
import org.mule.runtime.module.extension.tooling.internal.extension.TestConnectionProvider;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * A simple pojo containing reference information for making test around a {@link ExtensionDeclarer}
 * which represents a theoretical &quot;Web Service Consumer&quot; extension.
 * <p>
 * It contains an actual {@link ExtensionDeclarer} that can be accessed through the {@link #getExtensionDeclarer()}
 * method plus some other getters which provides access to other declaration components
 * that you might want to make tests against.
 * <p>
 * This case focuses on the scenario in which all sources, providers and operations are available
 * on all configs
 *
 * @since 1.0
 */
public class TestToolingExtensionDeclarer {

  public static final String CONFIG_NAME = "config";
  public static final String CONFIG_DESCRIPTION = "Default description";
  public static final String EXTENSION_NAME = "SDKToolingExtension";
  public static final String EXTENSION_DESCRIPTION = "Fake Extension Model for SDK tooling tests";
  public static final String VERSION = "3.6.0";
  public static final String MULESOFT = "MuleSoft";

  public static final String USERNAME_PARAM_NAME = "username";
  public static final String USERNAME_DESCRIPTION = "Authentication username";

  public static final String PASSWORD_PARAM_NAME = "password";
  public static final String PASSWORD_DESCRIPTION = "Authentication password";

  public static final String HOST_PARAM_NAME = "host";
  public static final String HOST_DESCRIPTION = "connection host";
  public static final String DEFAULT_HOST = "localhost";

  public static final String PORT_PARAM_NAME = "port";
  public static final String PORT_DESCRIPTION = "The connection port";

  public static final String CONNECTION_PROVIDER_NAME = "connection";
  public static final String CONNECTION_PROVIDER_DESCRIPTION = "my connection provider";

  public static final String PARTY_MODE_DESCRIPTION = "is it friday?";
  public static final String PARTY_MODE_PARAM_NAME = "partyMode";

  public static final String GREETINGS_PARAM_NAME = "greetings";
  public static final String GREETINGS_DESCRIPTION = "salutations";

  private ConnectionProviderFactory connectionProviderFactory;

  protected final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  protected final ClassTypeLoader classTypeLoader = new JavaTypeLoader(currentThread().getContextClassLoader());

  public ExtensionDeclarer declareOn(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarer.named(EXTENSION_NAME)
        .describedAs(EXTENSION_DESCRIPTION)
        .onVersion(VERSION)
        .fromVendor(MULESOFT)
        .withCategory(SELECT)
        .withXmlDsl(XmlDslModel.builder().build());

    ConfigurationDeclarer config =
        extensionDeclarer.withConfig(CONFIG_NAME)
            .describedAs(CONFIG_DESCRIPTION);

    ConnectionProviderDeclarer connectionProvider =
        config.withConnectionProvider(CONNECTION_PROVIDER_NAME)
            .describedAs(CONNECTION_PROVIDER_DESCRIPTION)
            .withConnectionManagementType(NONE);

    if (connectionProviderFactory != null) {
      connectionProvider.withModelProperty(new ConnectionProviderFactoryModelProperty(connectionProviderFactory));
    }

    ParameterGroupDeclarer parameterGroup = connectionProvider.onParameterGroup(DEFAULT_GROUP_NAME);
    parameterGroup.withRequiredParameter(USERNAME_PARAM_NAME).describedAs(USERNAME_DESCRIPTION).ofType(getStringType());
    parameterGroup.withRequiredParameter(PASSWORD_PARAM_NAME).describedAs(PASSWORD_DESCRIPTION).ofType(getStringType());
    parameterGroup.withOptionalParameter(
                                         HOST_PARAM_NAME)
        .describedAs(HOST_DESCRIPTION).ofType(getStringType()).defaultingTo(DEFAULT_HOST);
    parameterGroup.withRequiredParameter(PORT_PARAM_NAME).describedAs(PORT_DESCRIPTION).ofType(getNumberType());

    parameterGroup = connectionProvider.onParameterGroup(COMPLEX_PARAMETER_GROUP_NAME);
    TypeWrapper typeWrapper = new TypeWrapper(ComplexParameterGroup.class,
                                              new JavaTypeLoader(ComplexParameterGroup.class.getClassLoader()));
    parameterGroup.withModelProperty(new ParameterGroupModelProperty(
                                                                     new ParameterGroupDescriptor(COMPLEX_PARAMETER_GROUP_NAME,
                                                                                                  typeWrapper,
                                                                                                  typeWrapper.asMetadataType(),
                                                                                                  findField(TestConnectionProvider.class,
                                                                                                            "complexParameterGroup"),
                                                                                                  null)));

    parameterGroup.withRequiredParameter(PARTY_MODE_PARAM_NAME).describedAs(PARTY_MODE_DESCRIPTION).ofType(getBooleanType());

    parameterGroup.withOptionalParameter(GREETINGS_PARAM_NAME)
        .describedAs(GREETINGS_DESCRIPTION)
        .ofType(getListOfStringType())
        .withModelProperty(new NullSafeModelProperty(getListOfStringType()));

    MetadataType annoyingPojoType = classTypeLoader.load(AnnoyingPojo.class);
    parameterGroup.withOptionalParameter("annoyingPojo")
        .describedAs("annoying thing")
        .ofType(annoyingPojoType)
        .withModelProperty(new NullSafeModelProperty(annoyingPojoType));


    return extensionDeclarer;
  }

  public ExtensionDeclarer getExtensionDeclarer() {
    return declareOn(new ExtensionDeclarer());
  }

  public void setConnectionProviderFactory(ConnectionProviderFactory connectionProviderFactory) {
    this.connectionProviderFactory = connectionProviderFactory;
  }

  protected StringType getStringType() {
    return withType(typeBuilder.stringType(), String.class).build();
  }

  protected NumberType getNumberType() {
    return withType(typeBuilder.numberType(), Integer.class).build();
  }

  protected BinaryType getBinaryType() {
    return withType(typeBuilder.binaryType(), InputStream.class).build();
  }

  protected BooleanType getBooleanType() {
    return withType(typeBuilder.booleanType(), Boolean.class).build();
  }

  protected ObjectType getObjectType(Class<?> type) {
    return withType(typeBuilder.objectType(), type).build();
  }

  protected VoidType getVoidType() {
    return typeBuilder.voidType().build();
  }

  protected ArrayType getListOfStringType() {
    ArrayTypeBuilder arrayTypeBuilder = withType(typeBuilder.arrayType(), ArrayList.class);
    arrayTypeBuilder.of(getStringType());

    return arrayTypeBuilder.build();
  }

  private <T extends WithAnnotation<?>> T withType(T builder, Class<?> type) {
    builder = (T) builder.with(new TypeIdAnnotation(type.getName()));
    builder.with(new ClassInformationAnnotation(type));

    return builder;
  }
}
