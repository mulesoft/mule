/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.soap.SoapAttributes;
import org.mule.runtime.extension.api.soap.SoapOutputPayload;
import org.mule.runtime.extension.api.soap.WebServiceTypeKey;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeInputAttachmentsTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeInputHeadersTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeKeysResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeOutputTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeRequestTypeResolver;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.module.extension.soap.internal.runtime.operation.SoapOperationExecutorFactory;

import com.google.common.collect.ImmutableMap;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Declares the invoke operation for a given Soap Extension {@link ExtensionDeclarer}.
 *
 * @since 4.0
 */
public class SoapInvokeOperationDeclarer {

  public static final String MESSAGE_GROUP = "Message";
  private static final String KEYS_GROUP = "Web Service Configuration";
  static final String TRANSPORT = "Transport";
  static final String TRANSPORT_GROUP = TRANSPORT + " Configuration";
  static final String OPERATION_DESCRIPTION = "invokes Web Service operations";
  static final String OPERATION_NAME = "invoke";

  public static final String SERVICE_PARAM = "service";
  public static final String OPERATION_PARAM = "operation";
  public static final String HEADERS_PARAM = "headers";
  public static final String BODY_PARAM = "body";
  public static final String ATTACHMENTS_PARAM = "attachments";
  public static final String HEADERS_DISPLAY_NAME = "Headers";
  public static final String TRANSPORT_HEADERS_PARAM = "transportHeaders";

  private static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);
  public static final String SOAP_INVOKE_METADATA_CATEGORY = "SoapInvoke";

  /**
   * Declares the invoke operation.
   *
   * @param configDeclarer the soap config declarer
   * @param loader         a {@link ClassTypeLoader} to load some parameters types.
   * @param soapErrors     the {@link ErrorModel}s that this operation can throw.
   */
  void declare(ConfigurationDeclarer configDeclarer, ClassTypeLoader loader, Set<ErrorModel> soapErrors) {

    ReflectionCache reflectionCache = new ReflectionCache();

    OperationDeclarer operation = configDeclarer.withOperation(OPERATION_NAME)
        .describedAs(OPERATION_DESCRIPTION)
        .requiresConnection(true)
        .blocking(true)
        .withModelProperty(new CompletableComponentExecutorModelProperty(new SoapOperationExecutorFactory()))
        .withModelProperty(new ConnectivityModelProperty(ForwardingSoapClient.class));

    soapErrors.forEach(operation::withErrorModel);
    declareMetadata(operation, loader);
    declareOutput(operation, loader);
    declareMetadataKeyParameters(operation, loader, reflectionCache);
    declareRequestParameters(operation, loader);
  }

  private void declareMetadata(OperationDeclarer operation, ClassTypeLoader loader) {
    ImmutableMap.Builder<String, Supplier<? extends InputTypeResolver>> inputResolver = ImmutableMap.builder();
    inputResolver.put(BODY_PARAM, InvokeRequestTypeResolver::new);
    inputResolver.put(HEADERS_PARAM, InvokeInputHeadersTypeResolver::new);
    inputResolver.put(ATTACHMENTS_PARAM, InvokeInputAttachmentsTypeResolver::new);
    DefaultMetadataResolverFactory factory = new DefaultMetadataResolverFactory(InvokeKeysResolver::new,
                                                                                inputResolver.build(),
                                                                                InvokeOutputTypeResolver::new,
                                                                                NullMetadataResolver::new);
    operation.withModelProperty(new MetadataResolverFactoryModelProperty(() -> factory));
    operation.withModelProperty(new MetadataKeyIdModelProperty(loader.load(WebServiceTypeKey.class), KEYS_GROUP,
                                                               SOAP_INVOKE_METADATA_CATEGORY));
  }

  private void declareOutput(OperationDeclarer operation, ClassTypeLoader loader) {
    operation.withOutput().ofDynamicType(loader.load(SoapOutputPayload.class));
    operation.withOutputAttributes().ofType(loader.load(SoapAttributes.class));
  }

  /**
   * Given the Invoke Operation Declarer declares the parameters for the soap request.
   *
   * @param operation the invoke operation declarer.
   * @param loader    a {@link ClassTypeLoader} to load some parameters types.
   */
  private void declareRequestParameters(OperationDeclarer operation, ClassTypeLoader loader) {
    ParameterGroupDeclarer message = operation.onParameterGroup(MESSAGE_GROUP)
        .withDslInlineRepresentation(true)
        .withLayout(getLayout(1));

    MetadataType binaryType = loader.load(InputStream.class);
    ObjectType attachments = TYPE_BUILDER.objectType()
        .openWith(TYPE_BUILDER.binaryType()
            .id(InputStream.class.getName())
            .with(new TypedValueTypeAnnotation()))
        .with(new TypeIdAnnotation(Map.class.getName()))
        .build();

    message.withOptionalParameter(BODY_PARAM).ofDynamicType(binaryType)
        .withRole(PRIMARY_CONTENT)
        .defaultingTo(PAYLOAD)
        .withLayout(getLayout(3))
        .withDisplayModel(DisplayModel.builder()
            .summary("The XML body to include in the SOAP message, with all the required parameters.")
            .build());

    message.withOptionalParameter(HEADERS_PARAM).ofDynamicType(binaryType)
        .withRole(CONTENT)
        .withLayout(getLayout(4))
        .withDisplayModel(DisplayModel.builder()
            .displayName(HEADERS_DISPLAY_NAME)
            .summary("The XML headers to include in the SOAP message.")
            .build());

    message.withOptionalParameter(ATTACHMENTS_PARAM).ofDynamicType(attachments)
        .withRole(CONTENT)
        .withLayout(getLayout(5))
        .withDisplayModel(DisplayModel.builder()
            .summary("The attachments to include in the SOAP request.")
            .build());

    operation.onParameterGroup(TRANSPORT_GROUP).withLayout(getLayout(2))
        .withOptionalParameter(TRANSPORT_HEADERS_PARAM)
        .ofType(TYPE_BUILDER.objectType()
            .openWith(loader.load(String.class))
            .with(new TypeIdAnnotation(Map.class.getName()))
            .build())
        .withDsl(ParameterDslConfiguration.getDefaultInstance())
        .withLayout(LayoutModel.builder().order(2).tabName(TRANSPORT).build())
        .withDisplayModel(DisplayModel.builder()
            .displayName(HEADERS_DISPLAY_NAME)
            .summary("The headers to set in the transport configuration.")
            .build());
  }

  /**
   * Given the Invoke Operation Declarer declares all the parameters that the operation has.
   *
   * @param operation the invoke operation declarer.
   */
  private void declareMetadataKeyParameters(OperationDeclarer operation, ClassTypeLoader loader,
                                            ReflectionCache reflectionCache) {
    TypeWrapper keyType = new TypeWrapper(WebServiceTypeKey.class, loader);
    ParameterGroupDeclarer group = operation
        .onParameterGroup(KEYS_GROUP)
        .withModelProperty(
                           new ParameterGroupModelProperty(new ParameterGroupDescriptor(KEYS_GROUP, keyType)));

    StringType stringType = TYPE_BUILDER.stringType().build();
    group.withRequiredParameter(SERVICE_PARAM)
        .withModelProperty(new DeclaringMemberModelProperty(getField(WebServiceTypeKey.class, SERVICE_PARAM, reflectionCache)
            .get()))
        .ofType(stringType)
        .withModelProperty(new MetadataKeyPartModelProperty(1))
        .withLayout(getLayout(1));
    group.withRequiredParameter(OPERATION_PARAM)
        .ofType(stringType)
        .withModelProperty(new DeclaringMemberModelProperty(getField(WebServiceTypeKey.class, OPERATION_PARAM, reflectionCache)
            .get()))
        .withModelProperty(new MetadataKeyPartModelProperty(2))
        .withLayout(getLayout(2));
  }

  private LayoutModel getLayout(int order) {
    return LayoutModel.builder().order(order).build();
  }
}
