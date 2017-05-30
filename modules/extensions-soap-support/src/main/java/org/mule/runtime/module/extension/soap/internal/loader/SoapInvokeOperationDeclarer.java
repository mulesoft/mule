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
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.runtime.extension.internal.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.internal.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeInputAttachmentsTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeInputHeadersTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeKeysResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeOutputAttributesTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeOutputTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.InvokeRequestTypeResolver;
import org.mule.runtime.module.extension.soap.internal.metadata.WebServiceTypeKey;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.module.extension.soap.internal.runtime.operation.SoapOperationExecutorFactory;
import org.mule.runtime.soap.api.message.SoapMultipartPayload;

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

  private static final String KEYS_GROUP = "WebServiceConfiguration";
  static final String OPERATION_NAME = "invoke";
  static final String OPERATION_DESCRIPTION = "invokes Web Service operations";

  public static final String SERVICE_PARAM = "service";
  public static final String OPERATION_PARAM = "operation";
  public static final String HEADERS_PARAM = "headers";
  public static final String REQUEST_PARAM = "request";
  public static final String ATTACHMENTS_PARAM = "attachments";
  public static final String TRANSPORT_HEADERS_PARAM = "transportHeaders";

  private static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);

  /**
   * Declares the invoke operation.
   * @param declarer the soap extension declarer
   * @param loader   a {@link ClassTypeLoader} to load some parameters types.
   * @param soapErrors the {@link ErrorModel}s that this operation can throw.
   */
  public OperationDeclarer declare(ExtensionDeclarer declarer, ClassTypeLoader loader, Set<ErrorModel> soapErrors) {

    OperationDeclarer operation = declarer.withOperation(OPERATION_NAME)
        .describedAs(OPERATION_DESCRIPTION)
        .withModelProperty(new OperationExecutorModelProperty(new SoapOperationExecutorFactory()))
        .requiresConnection(true).withModelProperty(new ConnectivityModelProperty(ForwardingSoapClient.class));

    soapErrors.forEach(operation::withErrorModel);
    declareMetadata(operation, loader);
    declareOutput(operation, loader);
    declareGeneralParameters(operation, loader);
    declareMetadataKeyParameters(operation);
    return operation;
  }

  private void declareMetadata(OperationDeclarer operation, ClassTypeLoader loader) {
    ImmutableMap.Builder<String, Supplier<? extends InputTypeResolver>> inputResolver = ImmutableMap.builder();
    inputResolver.put(REQUEST_PARAM, InvokeRequestTypeResolver::new);
    inputResolver.put(HEADERS_PARAM, InvokeInputHeadersTypeResolver::new);
    inputResolver.put(ATTACHMENTS_PARAM, InvokeInputAttachmentsTypeResolver::new);
    DefaultMetadataResolverFactory factory = new DefaultMetadataResolverFactory(InvokeKeysResolver::new,
                                                                                inputResolver.build(),
                                                                                InvokeOutputTypeResolver::new,
                                                                                InvokeOutputAttributesTypeResolver::new);
    operation.withModelProperty(new MetadataResolverFactoryModelProperty(factory));
    operation.withModelProperty(new MetadataKeyIdModelProperty(loader.load(WebServiceTypeKey.class), KEYS_GROUP));
  }

  private void declareOutput(OperationDeclarer operation, ClassTypeLoader loader) {
    UnionTypeBuilder output = TYPE_BUILDER.unionType();
    output.id(Object.class.getName());
    output.of().stringType();
    output.of(loader.load(SoapMultipartPayload.class));
    operation.withOutput().ofDynamicType(output.build());
    operation.withOutputAttributes().ofDynamicType(TYPE_BUILDER.nullType().build());
  }

  /**
   * Given the Invoke Operation Declarer declares all the parameters that the operation has.
   *
   * @param operation the invoke operation declarer.
   * @param loader    a {@link ClassTypeLoader} to load some parameters types.
   */
  private void declareGeneralParameters(OperationDeclarer operation, ClassTypeLoader loader) {
    ParameterGroupDeclarer group = operation.blocking(true).onDefaultParameterGroup();
    StringType stringType = TYPE_BUILDER.stringType().build();
    BinaryType binary = TYPE_BUILDER.binaryType().with(new TypeIdAnnotation(InputStream.class.getName())).build();
    ObjectType attachments = TYPE_BUILDER.objectType()
        .openWith(loader.load(SoapAttachment.class))
        .with(new TypeIdAnnotation(Map.class.getName()))
        .build();

    group.withOptionalParameter(HEADERS_PARAM).ofDynamicType(binary).withRole(CONTENT).withLayout(getLayout(3));
    group.withOptionalParameter(REQUEST_PARAM).ofDynamicType(binary).withRole(PRIMARY_CONTENT).withLayout(getLayout(4));
    group.withOptionalParameter(ATTACHMENTS_PARAM).ofDynamicType(attachments).withRole(CONTENT).withLayout(getLayout(5));
    group.withOptionalParameter(TRANSPORT_HEADERS_PARAM).ofType(TYPE_BUILDER.objectType()
        .openWith(stringType)
        .with(new TypeIdAnnotation(Map.class.getName()))
        .build()).withLayout(getLayout(6));
  }

  /**
   * Given the Invoke Operation Declarer declares all the parameters that the operation has.
   *
   * @param operation the invoke operation declarer.
   */
  private void declareMetadataKeyParameters(OperationDeclarer operation) {
    TypeWrapper keyType = new TypeWrapper(WebServiceTypeKey.class);
    ParameterGroupDescriptor descriptor = new ParameterGroupDescriptor(KEYS_GROUP, keyType);
    ParameterGroupDeclarer group = operation.blocking(true)
        .onParameterGroup(KEYS_GROUP)
        .withModelProperty(new ParameterGroupModelProperty(descriptor));

    StringType stringType = TYPE_BUILDER.stringType().build();
    group.withRequiredParameter(SERVICE_PARAM)
        .withModelProperty(new DeclaringMemberModelProperty(getField(WebServiceTypeKey.class, SERVICE_PARAM).get()))
        .ofType(stringType)
        .withModelProperty(new MetadataKeyPartModelProperty(1))
        .withLayout(getLayout(1));
    group.withRequiredParameter(OPERATION_PARAM)
        .ofType(stringType)
        .withModelProperty(new DeclaringMemberModelProperty(getField(WebServiceTypeKey.class, OPERATION_PARAM).get()))
        .withModelProperty(new MetadataKeyPartModelProperty(2))
        .withLayout(getLayout(2));
  }

  private LayoutModel getLayout(int order) {
    return LayoutModel.builder().order(order).build();
  }
}
