/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.internal.util;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.TypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.java.api.handler.TypeHandlerManager;
import org.mule.metadata.java.api.utils.ParsingContext;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.factory.InMemoryCursorStreamProviderFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeHandlerManagerFactory;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.core.streaming.SimpleByteBufferManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.Matcher;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

public final class ExtensionsTestUtils {

  public static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  public static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);

  public static final String HELLO_WORLD = "Hello World!";

  private ExtensionsTestUtils() {}

  public static MetadataType toMetadataType(Class<?> type) {
    return TYPE_LOADER.load(type);
  }

  public static ProblemsReporter validate(Class<?> clazz, ExtensionModelValidator validator) {
    return validate(loadExtension(clazz), validator);
  }

  public static ProblemsReporter validate(ExtensionModel model, ExtensionModelValidator validator) {
    ProblemsReporter problemsReporter = new ProblemsReporter(model);
    validator.validate(model, problemsReporter);

    if (problemsReporter.hasErrors()) {
      throw new IllegalModelDefinitionException(problemsReporter.toString());
    }

    return problemsReporter;
  }

  public static ArrayType arrayOf(Class<? extends Collection> clazz, TypeBuilder itemType) {
    return TYPE_BUILDER.arrayType()
        .of(itemType)
        .with(new ClassInformationAnnotation(clazz))
        .build();
  }

  public static ObjectType dictionaryOf(TypeBuilder<?> valueTypeBuilder) {
    return TYPE_BUILDER.objectType().openWith(valueTypeBuilder)
        .with(new ClassInformationAnnotation(Map.class))
        .build();
  }

  public static TypeBuilder<?> objectTypeBuilder(Class<?> clazz) {
    BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
    final TypeHandlerManager typeHandlerManager = new ExtensionsTypeHandlerManagerFactory()
        .createTypeHandlerManager();
    typeHandlerManager.handle(clazz, new ParsingContext(), typeBuilder);

    return typeBuilder;
  }

  public static ValueResolver getResolver(Object value) throws Exception {
    return getResolver(value, null, true);
  }

  public static ValueResolver getResolver(Object value, ValueResolvingContext context, boolean dynamic,
                                          Class<?>... extraInterfaces)
      throws Exception {
    ValueResolver resolver;
    if (isEmpty(extraInterfaces)) {
      resolver = mock(ValueResolver.class);
    } else {
      resolver = mock(ValueResolver.class, withSettings().extraInterfaces(extraInterfaces));
    }

    when(resolver.resolve(context != null ? same(context) : any(ValueResolvingContext.class))).thenReturn(value);
    when(resolver.isDynamic()).thenReturn(dynamic);

    return resolver;
  }

  public static void assertMessageType(MetadataType type, MetadataType payloadType, MetadataType attributesType) {
    assertThat(type, is(instanceOf(MessageMetadataType.class)));
    assertThat(getId(type).get(), is(Message.class.getName()));

    MessageMetadataType messageType = (MessageMetadataType) type;
    assertThat(messageType.getPayloadType().get(), equalTo(payloadType));
    assertThat(messageType.getAttributesType().get(), equalTo(attributesType));
  }

  public static void assertMessageType(MetadataType type, Matcher payloadMatch, Matcher attributesMatcher) {
    assertThat(type, is(instanceOf(MessageMetadataType.class)));
    assertThat(getId(type).get(), is(Message.class.getName()));

    MessageMetadataType messageType = (MessageMetadataType) type;
    assertThat(messageType.getPayloadType().get(), payloadMatch);
    assertThat(messageType.getAttributesType().get(), attributesMatcher);
  }

  public static ParameterModel getParameter(String name, Class<?> type) {
    ParameterModel parameterModel = getParameter();
    when(parameterModel.getName()).thenReturn(name);
    when(parameterModel.getType()).thenReturn(toMetadataType(type));
    when(parameterModel.getLayoutModel()).thenReturn(empty());
    return parameterModel;
  }

  public static ParameterModel getParameter(String name, MetadataType metadataType) {
    ParameterModel parameterModel = getParameter();
    when(parameterModel.getName()).thenReturn(name);
    when(parameterModel.getType()).thenReturn(metadataType);
    return parameterModel;
  }

  private static ParameterModel getParameter() {
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getModelProperty(any())).thenReturn(Optional.empty());
    when(parameterModel.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(parameterModel.getRole()).thenReturn(BEHAVIOUR);
    return parameterModel;

  }

  public static void stubRegistryKeys(MuleContext muleContext, final String... keys) {
    when(((MuleContextWithRegistries) muleContext).getRegistry().get(anyString())).thenAnswer(invocation -> {
      String name = (String) invocation.getArguments()[0];
      if (name != null) {
        for (String key : keys) {
          if (name.contains(key)) {
            return null;
          }
        }
      }

      return RETURNS_DEEP_STUBS.get().answer(invocation);
    });
  }

  public static <C> C getConfigurationFromRegistry(String key, CoreEvent muleEvent, MuleContext muleContext)
      throws Exception {
    return (C) getConfigurationInstanceFromRegistry(key, muleEvent, muleContext).getValue();
  }

  public static ConfigurationInstance getConfigurationInstanceFromRegistry(String key, CoreEvent muleEvent,
                                                                           MuleContext muleContext)
      throws Exception {
    ExtensionManager extensionManager = muleContext.getExtensionManager();
    return extensionManager.getConfiguration(key, muleEvent);
  }

  /**
   * Receives to {@link String} representation of two XML files and verify that they are semantically equivalent
   *
   * @param expected the reference content
   * @param actual   the actual content
   * @throws Exception if comparison fails
   */
  public static void compareXML(String expected, String actual) throws Exception {
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    XMLUnit.setIgnoreAttributeOrder(false);

    Diff diff = XMLUnit.compareXML(expected, actual);
    if (!(diff.similar() && diff.identical())) {
      System.out.println(actual);
      DetailedDiff detDiff = new DetailedDiff(diff);
      @SuppressWarnings("rawtypes")
      List differences = detDiff.getAllDifferences();
      StringBuilder diffLines = new StringBuilder();
      for (Object object : differences) {
        Difference difference = (Difference) object;
        diffLines.append(difference.toString() + '\n');
      }

      throw new IllegalArgumentException("Actual XML differs from expected: \n" + diffLines.toString());
    }
  }

  public static void mockClassLoaderModelProperty(ExtensionModel extensionModel, ClassLoader classLoader) {
    when(extensionModel.getModelProperty(ClassLoaderModelProperty.class))
        .thenReturn(of(new ClassLoaderModelProperty(classLoader)));
  }

  public static void setRequires(EnrichableModel model, boolean requiresConfig, boolean requiresConnection) {
    if (requiresConfig) {
      when(model.getModelProperty(ConfigTypeModelProperty.class)).thenReturn(of(mock(ConfigTypeModelProperty.class)));
    }

    if (requiresConnection) {
      when(model.getModelProperty(ConnectivityModelProperty.class)).thenReturn(of(mock(ConnectivityModelProperty.class)));
    }
  }

  public static void mockSubTypes(ExtensionModel mockModel, SubTypesModel... subtypes) {
    if (isEmpty(subtypes)) {
      when(mockModel.getSubTypes()).thenReturn(emptySet());
    } else {
      when(mockModel.getSubTypes()).thenReturn(copyOf(subtypes));
    }
  }

  public static ParameterGroupModel mockParameters(ParameterizedModel parameterizedModel, ParameterModel... parameterModels) {
    return mockParameters(parameterizedModel, DEFAULT_GROUP_NAME, parameterModels);
  }

  public static ParameterGroupModel mockParameters(ParameterizedModel parameterizedModel, String groupName,
                                                   ParameterModel... parameterModels) {
    ParameterGroupModel group = mock(ParameterGroupModel.class);
    when(group.getName()).thenReturn(groupName);
    when(group.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    when(parameterizedModel.getParameterGroupModels()).thenReturn(asList(group));
    when(group.getParameterModels()).thenReturn(asList(parameterModels));
    when(parameterizedModel.getAllParameterModels()).thenReturn(asList(parameterModels));

    return group;
  }

  public static ParameterGroupDeclaration mockParameters(ParameterizedDeclaration declaration,
                                                         ParameterDeclaration... parameters) {
    return mockParameters(declaration, DEFAULT_GROUP_NAME, parameters);
  }

  public static ParameterGroupDeclaration mockParameters(ParameterizedDeclaration declaration, String groupName,
                                                         ParameterDeclaration... parameters) {
    ParameterGroupDeclaration group = mock(ParameterGroupDeclaration.class);
    when(group.getName()).thenReturn(groupName);
    when(declaration.getParameterGroups()).thenReturn(asList(group));
    when(declaration.getParameterGroup(groupName)).thenReturn(group);
    List<ParameterDeclaration> params = new ArrayList<>(asList(parameters));
    when(group.getParameters()).thenReturn(params);
    when(declaration.getAllParameters()).thenReturn(params);

    return group;
  }

  public static void mockExceptionEnricher(EnrichableModel enrichableModel, ExceptionHandlerFactory exceptionHandlerFactory) {
    Optional<ExceptionHandlerModelProperty> property = exceptionHandlerFactory != null
        ? of(new ExceptionHandlerModelProperty(exceptionHandlerFactory))
        : empty();

    when(enrichableModel.getModelProperty(ExceptionHandlerModelProperty.class)).thenReturn(property);
  }

  public static void mockInterceptors(EnrichableModel enrichableModel, List<InterceptorFactory> interceptorFactories) {
    if (interceptorFactories == null) {
      interceptorFactories = ImmutableList.of();
    }

    when(enrichableModel.getModelProperty(InterceptorsModelProperty.class))
        .thenReturn(of(new InterceptorsModelProperty(interceptorFactories)));
  }

  public static void mockConfigurationInstance(ConfigurationModel configurationModel, Object config) {
    ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
    when(configurationFactory.newInstance()).thenReturn(config);
    when(configurationFactory.getObjectType()).thenReturn((Class) config.getClass());

    when(configurationModel.getModelProperty(any())).thenAnswer(invocationOnMock -> {
      Class<? extends ModelProperty> propertyType = (Class<? extends ModelProperty>) invocationOnMock.getArguments()[0];
      if (ConfigurationFactoryModelProperty.class.equals(propertyType)) {
        return of(new ConfigurationFactoryModelProperty(configurationFactory));
      }

      return empty();
    });
  }

  public static void mockMetadataResolverFactory(EnrichableModel model, MetadataResolverFactory factory) {
    Optional<MetadataResolverFactoryModelProperty> property = factory != null
        ? of(new MetadataResolverFactoryModelProperty(() -> factory))
        : empty();

    when(model.getModelProperty(MetadataResolverFactoryModelProperty.class)).thenReturn(property);
  }

  public static void mockExecutorFactory(OperationModel operationModel, ComponentExecutorFactory operationExecutorFactory) {
    when(operationModel.getModelProperty(ComponentExecutorModelProperty.class))
        .thenReturn(of(new ComponentExecutorModelProperty(operationExecutorFactory)));
  }

  public static CursorStreamProviderFactory getDefaultCursorStreamProviderFactory(StreamingManager streamingManager) {
    return streamingManager.forBytes().getDefaultCursorProviderFactory();
  }

  public static CursorStreamProviderFactory getDefaultCursorStreamProviderFactory() {
    return new InMemoryCursorStreamProviderFactory(new SimpleByteBufferManager(),
                                                   InMemoryCursorStreamConfig.getDefault(),
                                                   mock(StreamingManager.class, Mockito.RETURNS_DEEP_STUBS));
  }

  public static void assertType(MetadataType metadataType, Class<?> expectedRawType,
                                Class<? extends MetadataType> typeQualifier) {
    assertThat(metadataType, is(instanceOf(typeQualifier)));
    getType(metadataType).ifPresent(type -> assertThat(expectedRawType.isAssignableFrom(type), is(true)));
  }

  public static void mockImplementingType(EnrichableModel model, Class<?> type) {
    when(model.getModelProperty(ImplementingTypeModelProperty.class))
        .thenReturn(java.util.Optional.of(new ImplementingTypeModelProperty(type)));
  }
}
