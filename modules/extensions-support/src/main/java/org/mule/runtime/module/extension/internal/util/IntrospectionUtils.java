/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isEnum;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isObjectType;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getId;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaParserUtils.toMuleApi;
import static org.mule.runtime.module.extension.api.loader.java.type.PropertyElement.Accessibility.READ_ONLY;
import static org.mule.runtime.module.extension.api.loader.java.type.PropertyElement.Accessibility.READ_WRITE;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.isIgnoreDisabled;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.spring.ByteBuddySpringCacheInstrumentator.instrumentSpringCachesForCleanup;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingType;
import static org.mule.runtime.module.extension.internal.util.ParameterGroupUtils.hasParameterGroupAnnotation;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PUBLIC;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.util.ReflectionUtilsPredicates.withAnnotation;
import static org.springframework.core.ResolvableType.NONE;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.ParameterResolverTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.loader.utils.JavaParserUtils;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.enricher.MetadataTypeEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectedFieldModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;
import org.mule.runtime.module.extension.internal.loader.java.property.RuntimeVersionModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.spring.ByteBuddySpringCachesManager;
import org.mule.sdk.api.annotation.param.NullSafe;
import org.mule.sdk.api.annotation.param.RuntimeVersion;
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.sdk.api.runtime.parameter.ParameterResolver;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.google.common.collect.ImmutableList;
import org.reflections.ReflectionUtils;
import org.springframework.core.ResolvableType;

/**
 * Set of utility operations to get insights about objects and their components
 *
 * @since 3.7.0
 */
public final class IntrospectionUtils {

  private static final AnyType ANY_TYPE = typeBuilder().anyType().build();
  private static final ArrayType ANY_ARRAY_TYPE = typeBuilder().arrayType().of(ANY_TYPE).build();
  private static final MetadataTypeEnricher enricher = new MetadataTypeEnricher();

  static {
    // A shade of the spring-core library is the cause for this additional cleanup.
    // If that shade is no longer present, this cleanup can be safely removed.
    instrumentSpringCachesForCleanup();
  }

  private IntrospectionUtils() {}

  /**
   * Returns a {@link MetadataType} representing the given {@link Class} type.
   *
   * @param type       the {@link Class} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   */
  public static MetadataType getMetadataType(Class<?> type, ClassTypeLoader typeLoader) {
    return typeLoader.load(ResolvableType.forClass(type).getType());
  }

  public static MetadataType getMetadataType(Type type) {
    MetadataType metadataType = type.asMetadataType();

    if (isAnyType(type)) {
      return enricher.enrich(ANY_TYPE, metadataType.getAnnotations());
    }

    if (isArrayOfAny(type)) {
      return enricher.enrich(typeBuilder().arrayType().of(typeBuilder().anyType()).build(), metadataType.getAnnotations());
    }

    return metadataType;
  }

  /**
   * Transforms a {@link MetadataType} and generates the correspondent {@link DataType}
   *
   * @param metadataType to introspect a create a {@link DataType} from it.
   * @return a {@link DataType} based on the given {@link MetadataType}
   */
  public static DataType toDataType(MetadataType metadataType) {
    Class<?> type = getType(metadataType).orElse(null);
    if (type == null) {
      return DataType.fromType(Object.class);
    }

    Reference<DataType> dataType = new Reference<>();

    metadataType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        dataType.set(DataType.fromType(type));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Optional<Class<Object>> optionalItemClass = getType(arrayType.getType());
        if (optionalItemClass.isPresent()) {
          Class<Object> itemClass = optionalItemClass.get();
          if (Collection.class.isAssignableFrom(type)) {
            dataType.set(DataType.builder()
                .collectionType((Class<? extends Collection>) type)
                .itemType(itemClass)
                .build());
          } else if (Iterator.class.isAssignableFrom(type)) {
            dataType.set(DataType.builder()
                .streamType((Class<? extends Iterator>) type)
                .itemType(itemClass)
                .build());
          } else {
            defaultVisit(arrayType);
          }
        } else {
          defaultVisit(arrayType);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (Map.class.isAssignableFrom(type)) {
          dataType.set(DataType.builder().mapType((Class<? extends Map>) type)
              .keyType(String.class)
              .valueType(objectType.getOpenRestriction()
                  .map(restriction -> {
                    if (restriction.getAnnotation(TypedValueTypeAnnotation.class).isPresent()) {
                      return TypedValue.class;
                    }
                    return getType(restriction).get();
                  })
                  .orElse(Object.class))
              .build());
        } else {
          defaultVisit(objectType);
        }
      }
    });

    return dataType.get();
  }

  /**
   * Based on a {@link ExtensionModel}, determines if this one has been created using the Java AST.
   *
   * @param model Extension model to introspect
   * @return a boolean indicating whether the {@link ExtensionModel} was created using the Java AST or using compiled classes
   */
  public static boolean isASTMode(ExtensionModel model) {
    Optional<ExtensionTypeDescriptorModelProperty> property = model.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    return !(property.isPresent() && property.get().getType().getDeclaringClass().isPresent());
  }

  /**
   * Based on a {@link BaseDeclaration}, determines if this one has been created using the Java AST.
   *
   * @param model Base Declaration to introspect
   * @return a boolean indicating whether the {@link BaseDeclaration} was created using the Java AST or using compiled classes
   */
  public static boolean isASTMode(BaseDeclaration model) {
    Optional<ExtensionTypeDescriptorModelProperty> property = model.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    return !(property.isPresent() && property.get().getType().getDeclaringClass().isPresent());
  }

  public static MetadataType getMethodReturnType(MethodElement method) {
    checkArgument(method != null, "Can't introspect a null method");
    return getReturnType(method.getReturnType());
  }

  /**
   * Returns the {@link MetadataType} for a source's output.
   * <p>
   * If the {@code type} is a collection of {@link Result} instances, then it will return an {@link ArrayType} which inner value
   * represent a {@link Message} which payload and attributes matches the types of the Result generics.
   *
   * @param returnType the source output type
   * @return a {@link MetadataType}
   */
  public static MetadataType getSourceReturnType(Type returnType) {
    return getReturnType(returnType);
  }

  public static MetadataType getReturnType(Type returnType) {
    Type type = returnType;

    if (returnType.isAssignableTo(Result.class) || returnType.isAssignableTo(org.mule.sdk.api.runtime.operation.Result.class)) {
      List<TypeGeneric> generics = returnType.getGenerics();
      if (generics.isEmpty()) {
        return ANY_TYPE;
      }
      Type payloadType = generics.get(0).getConcreteType();
      if (!payloadType.isAnyType()) {
        type = payloadType;
      } else {
        type = null;
      }
    }

    if (isPagingProvider(returnType)) {
      Type itemType = getPagingProviderTypes(returnType).getSecond();

      if (itemType.isSameType(Result.class) || itemType.isSameType(org.mule.sdk.api.runtime.operation.Result.class)) {
        return returnListOfMessagesType(returnType, itemType);
      } else {
        return typeBuilder().arrayType()
            .of(itemType.asMetadataType())
            .with(returnType.getClassInformation())
            .build();
      }
    }

    if (returnType.isAssignableTo(ParameterResolver.class) ||
        returnType.isAssignableTo(TypedValue.class) ||
        returnType.isAssignableTo(Literal.class)) {
      type = returnType.getGenerics().get(0).getConcreteType();
    }

    if (isCollection(returnType) && !returnType.getGenerics().isEmpty()) {
      Type itemType = returnType.getGenerics().get(0).getConcreteType();
      if (itemType.isAssignableTo(Result.class) || itemType.isAssignableTo(org.mule.sdk.api.runtime.operation.Result.class)) {
        return returnListOfMessagesType(returnType, itemType);
      }
    }

    if (isAnyType(returnType) && type != null) {
      return enricher.enrich(ANY_TYPE, type.asMetadataType().getAnnotations());
    }

    if (isArrayOfAny(returnType) && type != null) {
      return enricher.enrich(ANY_ARRAY_TYPE, type.asMetadataType().getAnnotations());
    }

    return type != null ? type.asMetadataType() : ANY_TYPE;
  }

  private static MetadataType returnListOfMessagesType(Type returnType,
                                                       Type resultType) {
    if (resultType.getGenerics().isEmpty()) {
      return getListOfMessageType(returnType, ANY_TYPE, ANY_TYPE);
    } else {
      TypeGeneric genericType = resultType.getGenerics().get(0);
      Type payloadType = genericType.getConcreteType();

      MetadataType outputType;

      if (payloadType.isAnyType()) {
        outputType = ANY_TYPE;
      } else {
        if (payloadType.isAssignableTo(TypedValue.class)) {
          payloadType = payloadType.getGenerics().get(0).getConcreteType();
        }
        outputType = payloadType.asMetadataType();
      }

      Type attributesType =
          resultType.getGenerics().get(1).getConcreteType();

      MetadataType attributesOutputType = attributesType.isAnyType()
          ? ANY_TYPE
          : attributesType.asMetadataType();

      return getListOfMessageType(returnType, outputType, attributesOutputType);
    }
  }

  private static ArrayType getListOfMessageType(Type returnType, MetadataType outputType, MetadataType attributesOutputType) {
    return typeBuilder().arrayType()
        .of(new MessageMetadataTypeBuilder()
            .with(new ClassInformationAnnotation(Message.class))
            .payload(outputType)
            .attributes(attributesOutputType)
            .build())
        .with(returnType.getClassInformation())
        .build();
  }

  private static boolean isCollection(Type type) {
    return type.isAssignableTo(Collection.class);
  }

  /**
   * Returns a {@link MetadataType} representing the {@link Result#getAttributes()} that will be set after executing the given
   * {@code method}.
   * <p>
   * If the {@code method} returns a {@link Result}, then it returns the type of the {@code Attributes} generic. In any other case
   * (including raw uses of {@link Result}) it will return a {@link VoidType}
   * <p>
   * If the {@code method} returns a collection or a {@link PagingProvider} of {@link Result}, then this will return
   * {@link VoidType} since the messages in the main output already contain an attributes for each item.
   *
   * @param method the {@link Method} being introspected
   * @return a {@link MetadataType}
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType getMethodReturnAttributesType(MethodElement method) {
    Type returnType = method.getReturnType();
    Type attributesType = null;

    if (returnType.isAssignableTo(Result.class)) {
      List<TypeGeneric> generics = returnType.getGenerics();
      if (generics.size() == 2) {
        Type genericType = generics.get(1).getConcreteType();
        if (genericType != null) {
          if (genericType.isAnyType()) {
            return ANY_TYPE;
          } else {
            attributesType = genericType;
          }
        } else {
          return ANY_TYPE;
        }
      } else {
        return ANY_TYPE;
      }
    }

    if (isPagingProvider(returnType)) {
      Type second = getPagingProviderTypes(returnType).getSecond();
      if (second.isSameType(Result.class)) {
        attributesType = null;
      }
    }

    if (isCollection(returnType)) {
      List<TypeGeneric> generics = returnType.getGenerics();

      if (generics.size() > 0) {
        Type genericType = generics.get(0).getConcreteType();
        if (genericType.isAssignableTo(Result.class)) {
          attributesType = null;
        }
      }
    }

    return attributesType != null ? attributesType.asMetadataType() : typeBuilder().voidType().build();
  }

  public static List<MetadataType> getGenerics(java.lang.reflect.Type type, ClassTypeLoader typeLoader) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      java.lang.reflect.Type[] generics = parameterizedType.getActualTypeArguments();
      return Stream.of(generics).map(typeLoader::load).collect(toList());
    }

    return new LinkedList<>();
  }

  /**
   * Determines if the given {@code type} implements any of the lifecycle annotations
   *
   * @param type the class to evaluate
   * @return whether it implements lifecycle or not
   */
  public static boolean isLifecycle(Class<?> type) {
    return Initialisable.class.isAssignableFrom(type)
        || Startable.class.isAssignableFrom(type)
        || Stoppable.class.isAssignableFrom(type)
        || Disposable.class.isAssignableFrom(type);
  }

  /**
   * Determines if the given {@code type} implements any of the lifecycle annotations
   *
   * @param type the class to evaluate
   * @return whether it implements lifecycle or not
   */
  public static boolean isLifecycle(Type type) {
    return type.isAssignableTo(Initialisable.class)
        || type.isAssignableTo(Startable.class)
        || type.isAssignableTo(Stoppable.class)
        || type.isAssignableTo(Disposable.class);
  }

  private static boolean isPagingProvider(Type type) {
    return type.isAssignableTo(PagingProvider.class);
  }

  private static BaseTypeBuilder typeBuilder() {
    return create(JAVA);
  }

  /**
   * Returns an array of {@link MetadataType} representing each of the given {@link Method}'s argument types.
   *
   * @param method     a not {@code null} {@link Method}
   * @param typeLoader a {@link ClassTypeLoader} to be used to create the returned {@link MetadataType}s
   * @return an array of {@link MetadataType} matching the method's arguments. If the method doesn't take any, then the array will
   *         be empty
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType[] getMethodArgumentTypes(Method method, ClassTypeLoader typeLoader) {
    checkArgument(method != null, "Can't introspect a null method");
    Class<?>[] parameters = method.getParameterTypes();
    if (isEmpty(parameters)) {
      return new MetadataType[] {};
    }

    MetadataType[] types = new MetadataType[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      ResolvableType type = ResolvableType.forMethodParameter(method, i);
      types[i] = typeLoader.load(type.getType());
    }

    return types;
  }

  /**
   * Returns a {@link MetadataType} describing the given {@link Field}'s type
   *
   * @param field      a not {@code null} {@link Field}
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType} matching the field's type
   * @throws IllegalArgumentException if field is {@code null}
   */
  public static MetadataType getFieldMetadataType(Field field, ClassTypeLoader typeLoader) {
    checkArgument(field != null, "Can't introspect a null field");
    return typeLoader.load(ResolvableType.forField(field).getType());
  }

  public static Optional<Field> getFieldByNameOrAlias(Class<?> clazz, String nameOrAlias, ReflectionCache reflectionCache) {
    Optional<Field> field = getField(clazz, nameOrAlias, reflectionCache);
    if (!field.isPresent()) {
      field = getAllFields(clazz, f -> getAlias(f).equals(nameOrAlias)).stream().findFirst();
    }

    return field;
  }

  public static Optional<Field> getField(Class<?> clazz, ParameterModel parameterModel, ReflectionCache reflectionCache) {
    return getField(clazz, getMemberName(parameterModel, parameterModel.getName()), reflectionCache);
  }

  public static Optional<Field> getField(Class<?> clazz, ParameterDeclaration parameterDeclaration,
                                         ReflectionCache reflectionCache) {
    return getField(clazz, MuleExtensionAnnotationParser.getMemberName(parameterDeclaration, parameterDeclaration.getName()),
                    reflectionCache);
  }

  public static Optional<Field> getField(Class<?> clazz, String name, ReflectionCache reflectionCache) {
    return reflectionCache.getFields(clazz).stream().filter(f -> f.getName().equals(name)).findFirst();
  }

  /**
   * Resolves and returns the field value of an object instance
   *
   * @param object          The object where grab the field value
   * @param fieldName       The name of the field to obtain the value
   * @param reflectionCache the cache for expensive reflection lookups
   * @return The value of the field with the given fieldName and object instance
   * @throws IllegalAccessException if is unavailable to access to the field
   * @throws NoSuchFieldException   if the field doesn't exist in the given object instance
   */
  public static Object getFieldValue(Object object, String fieldName, ReflectionCache reflectionCache)
      throws IllegalAccessException, NoSuchFieldException {
    final Optional<Field> fieldOptional = getField(object.getClass(), fieldName, reflectionCache);
    if (fieldOptional.isPresent()) {
      final Field field = fieldOptional.get();
      field.setAccessible(true);
      return field.get(object);
    } else {
      throw new NoSuchFieldException();
    }
  }

  public static <T extends EnrichableModel & NamedObject> String getMemberName(T enrichableNamedModel) {
    return getMemberName(enrichableNamedModel, enrichableNamedModel.getName());
  }

  public static String getMemberName(EnrichableModel enrichableModel, String defaultName) {
    return enrichableModel.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField().getName())
        .orElse(defaultName);
  }

  public static Optional<Field> getMemberField(EnrichableModel enrichableModel) {
    return enrichableModel.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField());
  }

  public static List<java.lang.reflect.Type> getInterfaceGenerics(final java.lang.reflect.Type type,
                                                                  final Class<?> implementedInterface) {

    ResolvableType interfaceType = null;
    ResolvableType searchClass = ResolvableType.forType(type);

    if (implementedInterface.equals(searchClass.getRawClass())) {
      return stream(searchClass.getGenerics()).map(resolvableType -> resolvableType.getType()).collect(toList());
    }

    Map<String, java.lang.reflect.Type> genericTypes = new SmallMap<>();

    addGenericsToMap(genericTypes, searchClass);

    ResolvableType baseImplementingClass = getInterfaceBaseImplementingClass(genericTypes, searchClass, implementedInterface);

    if (baseImplementingClass != null) {
      interfaceType = getInterfaceTypeFromClass(genericTypes, baseImplementingClass, implementedInterface);
    }

    if (interfaceType == null) {
      throw new IllegalArgumentException(format("Class '%s' does not implement the '%s' interface", type.getTypeName(),
                                                implementedInterface.getName()));
    }

    List<ResolvableType> generics = asList(interfaceType.getGenerics());

    return generics.stream().map(resolvableType -> {
      java.lang.reflect.Type genericType = resolvableType.getType();
      if (genericType instanceof TypeVariable) {
        genericType = genericTypes.get(resolvableType.toString());
      }
      return genericType;
    }).collect(toList());
  }

  private static ResolvableType getInterfaceTypeFromClass(Map<String, java.lang.reflect.Type> genericTypes,
                                                          ResolvableType implementingType, Class<?> implementedInterface) {
    while (!implementedInterface.equals(implementingType.getRawClass())
        && implementedInterface.isAssignableFrom(implementingType.getRawClass())) {
      for (ResolvableType innerIType : implementingType.getInterfaces()) {
        if (implementedInterface.isAssignableFrom(innerIType.getRawClass())) {
          addGenericsToMap(genericTypes, innerIType);
          implementingType = innerIType;
          break;
        }
      }
    }
    return implementedInterface.isAssignableFrom(implementingType.getRawClass()) ? implementingType : null;
  }

  private static ResolvableType getInterfaceBaseImplementingClass(Map<String, java.lang.reflect.Type> genericTypes,
                                                                  ResolvableType searchClass, Class<?> implementedInterface) {
    while (!Object.class.equals(searchClass.getRawClass()) && searchClass.getSuperType() != NONE
        && implementedInterface.isAssignableFrom(searchClass.getSuperType().getRawClass())) {
      searchClass = searchClass.getSuperType();
      addGenericsToMap(genericTypes, searchClass);
    }
    return implementedInterface.isAssignableFrom(searchClass.getRawClass()) ? searchClass : null;
  }

  private static void addGenericsToMap(Map<String, java.lang.reflect.Type> genericTypes, ResolvableType type) {
    ResolvableType[] generics = type.getGenerics();
    for (ResolvableType resolvableType : generics) {
      java.lang.reflect.Type foundType = resolvableType.getType();

      if (!(foundType instanceof TypeVariable)) {
        genericTypes.put(resolvableType.toString(), foundType);
      }
    }
  }

  public static List<Class<?>> findGenericsInSuperHierarchy(final Class<?> type) {
    if (Object.class.equals(type)) {
      return ImmutableList.of();
    }

    Class<?> superClass = type.getSuperclass();

    List<java.lang.reflect.Type> generics = getSuperClassGenerics(type, superClass);

    if (generics.isEmpty() && !Object.class.equals(superClass)) {
      return findGenericsInSuperHierarchy(superClass);
    }

    return (List) generics;
  }

  public static List<TypeMirror> getSuperClassGenerics(TypeElement type, Class superClass,
                                                       ProcessingEnvironment processingEnvironment) {
    TypeElement superClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(superClass.getName());
    TypeElement objectType = processingEnvironment.getElementUtils().getTypeElement(Object.class.getName());
    TypeMirror superClassTypeMirror = processingEnvironment.getTypeUtils().erasure(superClassTypeElement.asType());

    if (!processingEnvironment.getTypeUtils().isAssignable(type.asType(), superClassTypeMirror)) {
      throw new IllegalArgumentException(
                                         format("Class '%s' does not extend the '%s' class", type.getQualifiedName(),
                                                superClass.getSimpleName()));
    }

    DeclaredType searchClass = (DeclaredType) type.asType();
    while (!processingEnvironment.getTypeUtils().isAssignable(objectType.asType(), searchClass)) {
      if (processingEnvironment.getTypeUtils().isSameType(superClassTypeMirror,
                                                          processingEnvironment.getTypeUtils().erasure(searchClass))) {
        List<TypeMirror> typeArguments = (List<TypeMirror>) searchClass.getTypeArguments();
        return typeArguments;
      }

      TypeMirror superclass = ((TypeElement) searchClass.asElement()).getSuperclass();
      if (superclass instanceof DeclaredType) {
        searchClass = (DeclaredType) superclass;
      } else {
        searchClass = (DeclaredType) objectType.asType();
      }
    }
    return emptyList();
  }

  public static List<java.lang.reflect.Type> getSuperClassGenerics(java.lang.reflect.Type currentType, Class<?> superClass) {
    ResolvableType searchType = ResolvableType.forType(currentType);


    if (!superClass.isAssignableFrom(searchType.getRawClass())) {
      throw new IllegalArgumentException(
                                         format("Class '%s' does not extend the '%s' class",
                                                searchType.getRawClass().getCanonicalName(),
                                                superClass.getName()));
    }

    Map<String, java.lang.reflect.Type> genericTypes = new SmallMap<>();

    while (!Object.class.equals(searchType.getType())) {

      ResolvableType[] generics = searchType.getGenerics();

      for (ResolvableType resolvableType : generics) {
        java.lang.reflect.Type foundType = resolvableType.getType();

        if (!(foundType instanceof TypeVariable)) {
          genericTypes.put(resolvableType.toString(), foundType);
        }
      }

      if (superClass.equals(searchType.getRawClass())) {
        return stream(generics).map(resolvableType -> {
          java.lang.reflect.Type genericType = resolvableType.getType();
          if (genericType instanceof TypeVariable) {
            genericType = genericTypes.get(resolvableType.toString());
          }
          return genericType;
        }).collect(toList());
      } else {
        searchType = searchType.getSuperType();
      }
    }
    return new LinkedList<>();
  }

  public static void checkInstantiable(Class<?> declaringClass, ReflectionCache reflectionCache) {
    checkInstantiable(declaringClass, true, reflectionCache);
  }

  public static void checkInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor,
                                       ReflectionCache reflectionCache) {
    if (!isInstantiable(declaringClass, requireDefaultConstructor, reflectionCache)) {
      throw new IllegalArgumentException(format("Class %s cannot be instantiated.", declaringClass));
    }
  }

  public static boolean isInstantiable(MetadataType type, ReflectionCache reflectionCache) {
    return type.getAnnotation(ClassInformationAnnotation.class)
        .map(ClassInformationAnnotation::isInstantiable)
        .orElseGet(() -> getType(type)
            .map(t -> isInstantiable(t, reflectionCache))
            .orElse(false));
  }

  public static boolean isInstantiable(Class<?> declaringClass, ReflectionCache reflectionCache) {
    return isInstantiable(declaringClass, true, reflectionCache);
  }

  public static boolean isInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor,
                                       ReflectionCache reflectionCache) {
    return declaringClass != null && (!requireDefaultConstructor || reflectionCache.hasDefaultConstructor(declaringClass))
        && !declaringClass.isInterface() && !Modifier.isAbstract(declaringClass.getModifiers());
  }

  /**
   * Determines if the given {@code type} is assignable from any of the {@code matchingTypes}
   *
   * @param type          a {@link Class}
   * @param matchingTypes a collection of {@link Class classes} to test against
   * @return whether the type is assignable or not
   */
  public static boolean assignableFromAny(Class<?> type, Collection<Class<?>> matchingTypes) {
    return matchingTypes.stream().anyMatch(t -> t.isAssignableFrom(type));
  }

  public static boolean isRequired(AccessibleObject object) {
    return object.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) == null
        && object.getAnnotation(org.mule.sdk.api.annotation.param.Optional.class) == null;
  }

  public static boolean isRequired(ParameterModel parameterModel, boolean forceOptional) {
    return !forceOptional && parameterModel.isRequired();
  }

  public static boolean isVoid(Method method) {
    return isVoid(method.getReturnType());
  }

  public static boolean isVoid(ComponentModel componentModel) {
    return componentModel instanceof HasOutputModel
        && ((HasOutputModel) componentModel).getOutput().getType() instanceof VoidType;
  }

  private static boolean isVoid(Class<?> type) {
    return type.equals(void.class) || type.equals(Void.class);
  }

  public static boolean isVoid(MethodElement methodElement) {
    Type returnTypeElement = methodElement.getReturnType();
    return returnTypeElement.isAssignableFrom(void.class) || returnTypeElement.isAssignableFrom(Void.class);
  }

  public static Collection<Method> getApiMethods(Class<?> declaringClass) {
    return getMethodsStream(declaringClass)
        .filter(method -> !isLifecycleMethod(method))
        .collect(toCollection(LinkedHashSet::new));
  }

  public static Collection<ExecutableElement> getApiMethods(TypeElement typeElement,
                                                            ProcessingEnvironment processingEnvironment) {
    return getMethodsStream(typeElement, true, processingEnvironment)
        .filter(method -> !isLifecycleMethod(method, processingEnvironment))
        .collect(toCollection(LinkedHashSet::new));
  }

  /**
   * Determines if the referenced {@code element} should be ignored
   *
   * @param element a {@link WithAnnotations} declaration
   * @param context the introspection {@link ExtensionLoadingContext}
   * @return whether the source is ignored or not
   * @since 4.4.0
   */
  public static boolean isIgnored(WithAnnotations element, ExtensionLoadingContext context) {
    return isIgnoreDisabled(context)
        ? false
        : element.isAnnotatedWith(Ignore.class) || element.isAnnotatedWith(org.mule.sdk.api.annotation.Ignore.class);
  }

  private static boolean isLifecycleMethod(Method method) {
    return isLifecycleMethod(method, Initialisable.class, "initialise")
        || isLifecycleMethod(method, Startable.class, "start")
        || isLifecycleMethod(method, Stoppable.class, "stop")
        || isLifecycleMethod(method, Disposable.class, "dispose");
  }

  private static boolean isLifecycleMethod(ExecutableElement method, ProcessingEnvironment processingEnvironment) {
    return isLifecycleMethod(method, Initialisable.class, "initialise", processingEnvironment)
        || isLifecycleMethod(method, Startable.class, "start", processingEnvironment)
        || isLifecycleMethod(method, Stoppable.class, "stop", processingEnvironment)
        || isLifecycleMethod(method, Disposable.class, "dispose", processingEnvironment);
  }

  private static boolean isLifecycleMethod(Method method, Class<?> lifecycleClass, String lifecycleMethodName) {
    return lifecycleClass.isAssignableFrom(method.getDeclaringClass()) && method.getName().equals(lifecycleMethodName);
  }

  private static boolean isLifecycleMethod(ExecutableElement method, Class<?> lifecycleClass, String lifecycleMethodName,
                                           ProcessingEnvironment processingEnvironment) {
    TypeElement lifecycleElement = processingEnvironment.getElementUtils().getTypeElement(lifecycleClass.getTypeName());
    return processingEnvironment.getTypeUtils().isAssignable(method.getEnclosingElement().asType(), lifecycleElement.asType())
        && method.getSimpleName().toString().equals(lifecycleMethodName);
  }

  /**
   * Returns all the methods in the {@code declaringClass} which are annotated with {@code annotationType}, including those
   * declared in super classes.
   *
   * @param declaringClass the type to introspect
   * @param annotationType the annotation you're looking for
   * @return a {@link Collection} of {@link Method}s
   */
  public static Collection<Method> getMethodsAnnotatedWith(Class<?> declaringClass, Class<? extends Annotation> annotationType) {
    return getMethodsAnnotatedWith(declaringClass, annotationType, true);
  }

  /**
   * Returns all the methods in the {@code declaringClass} which are annotated with {@code annotationType}
   *
   * @param declaringClass the type to introspect
   * @param annotationType the annotation you're looking for
   * @param superClasses   whether to consider supper classes or not
   * @return a {@link Collection} of {@link Method}s
   */
  public static Collection<Method> getMethodsAnnotatedWith(Class<?> declaringClass,
                                                           Class<? extends Annotation> annotationType,
                                                           boolean superClasses) {
    return getMethodsStream(declaringClass, superClasses)
        .filter(method -> method.getAnnotation(annotationType) != null)
        .collect(toCollection(LinkedHashSet::new));
  }

  private static Stream<Method> getMethodsStream(Class<?> declaringClass) {
    return getMethodsStream(declaringClass, true);
  }

  private static Stream<Method> getMethodsStream(Class<?> declaringClass, boolean superClasses) {
    Stream<Method> methodStream;
    if (superClasses) {
      methodStream = Stream.concat(Stream.of(declaringClass.getDeclaredMethods()),
                                   ReflectionUtils.getAllSuperTypes(declaringClass).stream()
                                       .filter(type -> !type.isInterface())
                                       .flatMap(type -> Stream.of(type.getDeclaredMethods())));
    } else {
      methodStream = Stream.of(declaringClass.getDeclaredMethods());
    }

    return methodStream.filter(method -> isPublic(method.getModifiers()));
  }

  public static Stream<ExecutableElement> getMethodsStream(TypeElement typeElement, boolean superClasses,
                                                           ProcessingEnvironment processingEnvironment) {
    Stream<ExecutableElement> methodStream;

    if (superClasses) {
      methodStream = getAllSuperTypes(typeElement, processingEnvironment).stream()
          .flatMap(IntrospectionUtils::getEnclosingMethodsStream);
    } else {
      methodStream = getEnclosingMethodsStream(typeElement);
    }

    return methodStream.filter(method -> method.getModifiers().contains(PUBLIC));
  }

  public static Stream<ExecutableElement> getEnclosingMethodsStream(TypeElement typeElement) {
    return typeElement.getEnclosedElements().stream().filter(elem -> elem.getKind().equals(METHOD))
        .map(elem -> (ExecutableElement) elem);
  }

  private static List<TypeElement> getAllSuperTypes(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    TypeElement objectType = processingEnvironment.getElementUtils().getTypeElement(Object.class.getTypeName());
    LinkedList<TypeElement> typeElements = new LinkedList<>();
    TypeElement currentType = typeElement;
    while (currentType != null && !objectType.equals(currentType)) {
      typeElements.addFirst(currentType);
      currentType = (TypeElement) processingEnvironment.getTypeUtils().asElement(currentType.getSuperclass());
    }

    return typeElements;
  }

  /**
   * Returns a {@link Stream} with the fields annotated with any of the given {@code annotationTypes}
   *
   * @param clazz           The class to instrospect
   * @param annotationTypes the annotation types
   * @return a {@link Stream}
   * @since 4.5.0
   */
  public static Stream<Field> getAnnotatedFieldsStream(Class<?> clazz, Class<? extends Annotation>... annotationTypes) {
    return getDescendingHierarchy(clazz).stream().flatMap(type -> stream(type.getDeclaredFields()))
        .filter(field -> Stream.of(annotationTypes).anyMatch(annotationType -> field.getAnnotation(annotationType) != null));
  }

  public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation>... annotationTypes) {
    return getAnnotatedFieldsStream(clazz, annotationTypes).collect(toImmutableList());
  }

  public static List<Field> getFields(Class<?> clazz) {
    try {
      return getFieldsStream(clazz).collect(toImmutableList());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static List<VariableElement> getFields(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    try {
      return getFieldsStream(typeElement, processingEnvironment).collect(toImmutableList());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static Stream<VariableElement> getFieldsStream(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    try {
      return getAllSuperTypes(typeElement, processingEnvironment).stream()
          .flatMap(elem -> elem.getEnclosedElements().stream())
          .filter(elem -> elem.getKind() == ElementKind.FIELD)
          .map(varElement -> (VariableElement) varElement);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static Stream<Field> getFieldsStream(Class<?> clazz) {
    try {
      return getDescendingHierarchy(clazz).stream().flatMap(type -> {
        Field[] declaredFields;
        try {
          declaredFields = type.getDeclaredFields();
        } catch (LinkageError e) {
          throw new IllegalStateException("Exception getting declaredFields of type '" + clazz.getName() + "'", e);
        }
        return stream(declaredFields);
      });
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Field> getFieldsOfType(Class<?> introspectedType, Class fieldType) {
    return getFieldsStream(introspectedType)
        .filter(f -> fieldType.isAssignableFrom(f.getType()) && !isStatic(f.getModifiers()))
        .collect(toList());
  }

  /**
   * Returns the {@link Alias} or {@link org.mule.sdk.api.annotation.Alias} name of the given {@code field}.
   * <p>
   * If the element doesn't have an alias, then the default name is returned
   *
   * @param field the introspected field
   * @return an alias name
   */
  public static String getAlias(Field field) {
    return JavaParserUtils.getAlias(field);
  }

  /**
   * @param field a field
   * @return whether the given {@code field} is a config override
   */
  public static boolean isConfigOverride(Field field) {
    return JavaParserUtils.isConfigOverride(field);
  }

  /**
   * @param field a field
   * @return {@link NullSafe#defaultImplementingType()} {@link Optional} or
   *         {@link org.mule.runtime.extension.api.annotation.param.NullSafe#defaultImplementingType()} {@link Optional} in case
   *         the given {@link Field field} is annotated.
   */
  public static Optional<Class<?>> getNullSafeDefaultImplementedType(Field field) {
    return JavaParserUtils.getNullSafeDefaultImplementedType(field);
  }

  private static List<Class<?>> getDescendingHierarchy(Class<?> type) {
    List<Class<?>> types = new LinkedList<>();
    types.add(type);
    for (type = type.getSuperclass(); type != null && !Object.class.equals(type); type = type.getSuperclass()) {
      types.add(0, type);
    }

    return ImmutableList.copyOf(types);
  }

  public static Collection<Field> getExposedFields(Class<?> extensionType, ReflectionCache reflectionCache) {
    Collection<Field> allFields = getAnnotatedFields(extensionType, Parameter.class);
    if (!allFields.isEmpty()) {
      return allFields;
    }
    return getFieldsWithGetters(extensionType, reflectionCache);
  }

  public static Set<Field> getFieldsWithGetters(Class<?> extensionType, ReflectionCache reflectionCache) {
    return getPropertyDescriptors(extensionType).stream().filter(p -> p.getReadMethod() != null)
        .map(p -> getField(extensionType, p.getName(), reflectionCache)).filter(Optional::isPresent).map(Optional::get)
        .collect(toSet());
  }

  public static List<FieldElement> getFieldsWithGetters(Type extensionType) {
    Set<String> properties = extensionType.getProperties().stream()
        .filter(p -> p.getAccess().equals(READ_ONLY) || p.getAccess().equals(READ_WRITE))
        .map(p -> p.getName()).collect(toSet());

    return extensionType.getFields()
        .stream()
        .filter(field -> properties.contains(field.getName()))
        .collect(toList());
  }

  public static List<PropertyDescriptor> getPropertyDescriptors(Class<?> extensionType) {
    try {
      PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(extensionType).getPropertyDescriptors();
      return asList(propertyDescriptors);
    } catch (IntrospectionException e) {
      throw new IllegalModelDefinitionException("Could not introspect POJO: " + extensionType.getName(), e);
    }
  }

  public static Optional<ExpressionSupport> getExpressionSupport(WithAnnotations annotatedElement,
                                                                 String elementType,
                                                                 String elementName) {
    return mapReduceSingleAnnotation(
                                     annotatedElement,
                                     elementType,
                                     elementName,
                                     Expression.class,
                                     org.mule.sdk.api.annotation.Expression.class,
                                     value -> value.getEnumValue(Expression::value),
                                     value -> toMuleApi(value.getEnumValue(org.mule.sdk.api.annotation.Expression::value)));
  }

  public static String getSourceName(Class<?> sourceType) {
    return JavaParserUtils.getAlias(sourceType);
  }

  /**
   * Looks for the annotation in the given class. If the annotation is not found, it keeps looking recursively for it in the
   * superClass until it finds it or there is no superClass to analyze.
   */
  public static <T extends Annotation> T getAnnotation(Class<?> annotatedClass, Class<T> annotationClass) {
    T annotation = annotatedClass.getAnnotation(annotationClass);
    Class<?> superClass = annotatedClass.getSuperclass();
    while (annotation == null && superClass != null && !superClass.equals(Object.class)) {
      annotation = superClass.getAnnotation(annotationClass);
      superClass = superClass.getSuperclass();
    }
    return annotation;
  }

  /**
   * Traverses through all the {@link ParameterModel}s of the {@code extensionModel} and returns the {@link Class classes} that
   * are modeled by each parameter's {@link ParameterModel#getType()}.
   * <p>
   * This includes every single {@link ParameterModel} in the model, including configs, providers, operations, etc.
   *
   * @param extensionModel a {@link ExtensionModel}
   * @return a non {@code null} {@link Set}
   */
  public static Set<Class<?>> getParameterClasses(ExtensionModel extensionModel, ClassLoader extensionClassLoader) {
    Set<Class<?>> parameterClasses = new HashSet<>();
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        parameterClasses.addAll(collectRelativeClasses(model.getType(), extensionClassLoader));
      }
    }.walk(extensionModel);

    return parameterClasses;
  }

  /**
   * Traverses through all the {@link ObjectType object types} of the {@link ExtensionModel extension model's}
   * {@link SubTypesModel} and returns the {@link Class classes} that are used behind of each type.
   *
   * @param extensionModel a {@link ExtensionModel}
   * @return a non {@code null} {@link Set}
   * @since 4.1
   */
  public static Set<Class<?>> getSubtypeClasses(ExtensionModel extensionModel, ClassLoader extensionClassLoader) {
    return extensionModel.getSubTypes().stream().flatMap(subTypesModel -> {
      Set<Class<?>> classes = new HashSet<>();
      classes.addAll(collectRelativeClasses(subTypesModel.getBaseType(), extensionClassLoader));
      classes.addAll(subTypesModel.getSubTypes()
          .stream()
          .flatMap(type -> collectRelativeClasses(type, extensionClassLoader)
              .stream())
          .collect(toSet()));
      return classes.stream();
    }).collect(toSet());
  }

  /**
   * Given a {@link MetadataType} it adds all the {@link Class} that are related from that type. This includes generics of an
   * {@link ArrayType}, open restriction of an {@link ObjectType} as well as its fields.
   *
   * @param type                 {@link MetadataType} to inspect
   * @param extensionClassLoader extension class loader
   * @return {@link Set<Class<?>>} with the classes reachable from the {@code type}
   */
  public static Set<Class<?>> collectRelativeClasses(MetadataType type, ClassLoader extensionClassLoader) {
    Set<Class<?>> relativeClasses = new HashSet<>();
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObjectField(ObjectFieldType objectFieldType) {
        objectFieldType.getValue().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (objectType.getMetadataFormat() != JAVA) {
          return;
        }

        final Class<Object> clazz = getType(objectType).orElse(null);
        if (clazz == null || relativeClasses.contains(clazz)) {
          return;
        }

        Optional<ClassInformationAnnotation> classInformation = objectType.getAnnotation(ClassInformationAnnotation.class);
        if (classInformation.isPresent()) {
          classInformation.get().getGenericTypes()
              .forEach(generic -> relativeClasses.add(loadClass(generic, extensionClassLoader)));
        }

        relativeClasses.add(clazz);
        objectType.getFields().stream().forEach(objectFieldType -> objectFieldType.accept(this));
        objectType.getOpenRestriction().ifPresent(t -> t.accept(this));
      }

      @Override
      public void visitString(StringType stringType) {
        if (stringType.getMetadataFormat() == JAVA && isEnum(stringType)) {
          getType(stringType).ifPresent(relativeClasses::add);
        }
      }
    });

    return relativeClasses;
  }

  /**
   * Given a {@link MetadataType} it adds all the {@link Class} that are related from that type. This includes generics of an
   * {@link ArrayType}, open restriction of an {@link ObjectType} as well as its fields.
   *
   * @param type {@link MetadataType} to inspect
   * @return {@link Set<Class>>} with the classes reachable from the {@code type}
   */
  public static Set<String> collectRelativeClassesAsString(MetadataType type) {
    Set<String> relativeClasses = new HashSet<>();
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObjectField(ObjectFieldType objectFieldType) {
        objectFieldType.getValue().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (objectType.getMetadataFormat() != JAVA) {
          return;
        }

        final String clazz = getId(objectType).orElse(null);
        if (clazz == null || relativeClasses.contains(clazz)) {
          return;
        }

        Optional<ClassInformationAnnotation> classInformation = objectType.getAnnotation(ClassInformationAnnotation.class);
        classInformation
            .ifPresent(classInformationAnnotation -> relativeClasses
                .addAll(classInformationAnnotation.getGenericTypes()));

        relativeClasses.add(clazz);
        objectType.getFields().forEach(objectFieldType -> objectFieldType.accept(this));
        objectType.getOpenRestriction().ifPresent(t -> t.accept(this));
      }

      @Override
      public void visitString(StringType stringType) {
        if (stringType.getMetadataFormat() == JAVA && isEnum(stringType)) {
          getId(stringType).ifPresent(relativeClasses::add);
        }
      }
    });

    return relativeClasses;
  }

  private static Class loadClass(String name, ClassLoader extensionClassloader) {
    try {
      return ClassUtils.loadClass(name, extensionClassloader);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }


  /**
   * Given a {@link Set} of Annotation classes and a {@link MetadataType} that describes a component parameter, indicates if the
   * parameter is considered as a multilevel {@link MetadataKeyId}
   *
   * @param annotations   of the parameter
   * @param parameterType of the parameter
   * @return a boolean indicating if the Parameter is considered as a multilevel {@link MetadataKeyId}
   */
  public static boolean isMultiLevelMetadataKeyId(Set<Class<? extends Annotation>> annotations, MetadataType parameterType) {
    return annotations.contains(MetadataKeyId.class) && isObjectType(parameterType);
  }

  /**
   * Given a {@link Set} of annotation classes and a {@link MetadataType} of a component parameter, indicates if the parameter is
   * a parameter container.
   * <p>
   * To be a parameter container means that the parameter is a {@link ParameterGroup} or a multilevel {@link MetadataKeyId}.
   *
   * @param annotations   of the component parameter
   * @param parameterType of the component parameter
   * @return a boolean indicating if the parameter is considered as a parameter container
   */
  public static boolean isParameterContainer(Set<Class<? extends Annotation>> annotations, MetadataType parameterType) {
    return hasParameterGroupAnnotation(annotations) || isMultiLevelMetadataKeyId(annotations, parameterType);
  }

  public static java.util.Optional<AnnotatedElement> getAnnotatedElement(BaseDeclaration<?> declaration) {
    final java.util.Optional<DeclaringMemberModelProperty> declaringMember =
        declaration.getModelProperty(DeclaringMemberModelProperty.class);
    final java.util.Optional<ImplementingParameterModelProperty> implementingParameter =
        declaration.getModelProperty(ImplementingParameterModelProperty.class);

    AnnotatedElement annotatedElement = null;
    if (declaringMember.isPresent()) {
      annotatedElement = declaringMember.get().getDeclaringField();
    }

    if (implementingParameter.isPresent()) {
      annotatedElement = implementingParameter.get().getParameter();
    }

    return java.util.Optional.ofNullable(annotatedElement);
  }

  public static String getContainerName(AnnotatedElement container) {
    if (container instanceof Field) {
      return ((Field) container).getName();
    } else if (container instanceof java.lang.reflect.Parameter) {
      return ((java.lang.reflect.Parameter) container).getName();
    } else {
      throw new IllegalArgumentException("Unknown container type");
    }
  }

  public static String getGroupModelContainerName(ParameterGroupModel groupModel) {
    return groupModel.getModelProperty(ParameterGroupModelProperty.class)
        .map(modelProperty -> getContainerName(modelProperty.getDescriptor().getContainer()))
        .orElse(groupModel.getName());
  }

  /**
   * Resolves the original name of a parameter before it was modified due to the usage of an Alias.
   *
   * @param parameterDeclaration parameter from which look for their original name
   * @return the original name
   */
  public static String getImplementingName(ParameterDeclaration parameterDeclaration) {
    return getImplementingName(parameterDeclaration.getName(),
                               () -> parameterDeclaration.getModelProperty(ImplementingParameterModelProperty.class),
                               () -> parameterDeclaration.getModelProperty(DeclaringMemberModelProperty.class));
  }

  /**
   * Resolves the original name of a parameter before it was modified due to the usage of an Alias.
   *
   * @param parameterModel parameter from which look for their original name
   * @return the original name
   */
  public static String getImplementingName(ParameterModel parameterModel) {
    return getImplementingName(parameterModel.getName(),
                               () -> parameterModel.getModelProperty(ImplementingParameterModelProperty.class),
                               () -> parameterModel.getModelProperty(DeclaringMemberModelProperty.class));
  }

  private static String getImplementingName(String originalName,
                                            Supplier<Optional<ImplementingParameterModelProperty>> implementingParameter,
                                            Supplier<Optional<DeclaringMemberModelProperty>> declaringMember) {
    Optional<ImplementingParameterModelProperty> parameter = implementingParameter.get();
    if (parameter.isPresent()) {
      return parameter.get().getParameter().getName();
    }

    Optional<DeclaringMemberModelProperty> field = declaringMember.get();
    if (field.isPresent()) {
      return field.get().getDeclaringField().getName();
    }

    return originalName;
  }

  public static boolean isParameterResolver(MetadataType metadataType) {
    return metadataType.getAnnotation(ParameterResolverTypeAnnotation.class).isPresent();
  }

  public static boolean isLiteral(MetadataType metadataType) {
    return metadataType.getAnnotation(LiteralTypeAnnotation.class).isPresent();
  }

  public static boolean isTypedValue(MetadataType metadataType) {
    return metadataType.getAnnotation(TypedValueTypeAnnotation.class).isPresent();
  }

  /**
   * Resolves the correspondent {@link ConnectionProviderModel} for a given {@link ConnectionProvider} instance.
   *
   * @param connectionProvider     connection provider class
   * @param allConnectionProviders list of available {@link ConnectionProviderModel}
   * @return an {@link Optional} value of the {@link ConnectionProviderModel}
   */
  public static Optional<ConnectionProviderModel> getConnectionProviderModel(
                                                                             Class<? extends ConnectionProvider> connectionProvider,
                                                                             List<ConnectionProviderModel> allConnectionProviders) {
    for (ConnectionProviderModel providerModel : allConnectionProviders) {
      Optional<Class> providerImplementingType = getImplementingType(providerModel);

      if (providerImplementingType.map(type -> type.equals(connectionProvider)).orElse(false)) {
        return of(providerModel);
      }

    }
    return empty();
  }

  private static void injectFieldFromModelProperty(Object target, Object value,
                                                   Optional<? extends InjectedFieldModelProperty> modelProperty,
                                                   Class<? extends Annotation> annotationClass) {
    if (value == null || modelProperty == null) {
      return;
    }

    modelProperty.ifPresent(property -> {
      final Field field = property.getField();

      if (!field.getDeclaringClass().isInstance(target)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("field '%s' is annotated with @%s but not defined on an instance of type '%s'",
                                                                      field.toString(),
                                                                      annotationClass.getSimpleName(),
                                                                      target.getClass().getName()));
      }
      new FieldSetter<>(field).set(target, value);
    });
  }

  private static Optional<FieldSetter> getFieldSetterForAnnotatedField(Object target,
                                                                       Class<? extends Annotation> annotationClass,
                                                                       ReflectionCache reflectionCache) {
    return reflectionCache.getFieldSetterForAnnotatedField(target, annotationClass);
  }

  private static void injectFieldOfType(Object target, Object value, Class<?> fieldType) {
    final Class<?> type = target.getClass();
    List<Field> fields = getFieldsOfType(type, fieldType);
    if (fields.isEmpty()) {
      return;
    } else if (fields.size() > 1) {
      throw new IllegalModelDefinitionException(format(
                                                       "Class '%s' has %d fields of type with @%s. Only one field of that type was expected",
                                                       type.getName(), fields.size(), fieldType));
    }

    new FieldSetter<>(fields.get(0)).set(target, value);
  }

  /**
   * Sets the {@code configName}, {@code encoding} and {@link MuleVersion} into the fields of the target annotated with
   * {@link RefName}, {@link DefaultEncoding} and {@link RuntimeVersion} respectively if present and the {@code model} contains
   * the {@link DeclaringMemberModelProperty}.
   *
   * @param model       enriched with {@link InjectedFieldModelProperty}
   * @param target      object in which the fields are going to be set
   * @param configName  to be injected into the {@link String} field annotated with {@link RefName}
   * @param encoding    to be injected into the {@link String} field annotated with {@link DefaultEncoding}
   * @param muleVersion to be injected into the {@link MuleVersion} field annotated with {@link RuntimeVersion}
   */
  public static void injectFields(EnrichableModel model, Object target, String configName, String encoding,
                                  MuleVersion muleVersion) {
    injectFieldFromModelProperty(target, configName, model.getModelProperty(RequireNameField.class), RefName.class);
    injectDefaultEncoding(model, target, encoding);
    injectRuntimeVersion(model, target, muleVersion);
  }

  /**
   * Sets the {@code encoding} value into the field of the {@code target} annotated {@link DefaultEncoding} if the {@code model}
   * contains the {@link DeclaringMemberModelProperty} property and the value is not {@code null}.
   *
   * @param model    enriched with {@link DefaultEncodingModelProperty}
   * @param target   object in which the fields are going to be set
   * @param encoding to be injected into the {@link String} field annotated with {@link DefaultEncoding}
   */
  public static void injectDefaultEncoding(EnrichableModel model, Object target, String encoding) {
    injectFieldFromModelProperty(target, encoding, model.getModelProperty(DefaultEncodingModelProperty.class),
                                 DefaultEncoding.class);
  }

  /**
   * Sets the {@link MuleVersion} into the field of the {@code target} annotated {@link RuntimeVersion} if the {@code model}
   * contains the {@link DeclaringMemberModelProperty} property and the value is not {@code null}.
   *
   * @param model       enriched with {@link DefaultEncodingModelProperty}
   * @param target      object in which the fields are going to be set
   * @param muleVersion {@link MuleVersion} to be injected into the {@link MuleVersion} field annotated with
   *                    {@link RuntimeVersion}
   */
  public static void injectRuntimeVersion(EnrichableModel model, Object target, MuleVersion muleVersion) {
    injectFieldFromModelProperty(target, muleVersion, model.getModelProperty(RuntimeVersionModelProperty.class),
                                 RuntimeVersion.class);
  }

  /**
   * Sets the {@code configName}, {@code encoding} and {@link MuleVersion} into the fields of the target annotated with
   * {@link RefName}, {@link DefaultEncoding} and {@link RuntimeVersion} respectively if present.
   *
   * @param target          object in which the fields are going to be set
   * @param configName      to be injected into the {@link String} field annotated with {@link RefName}
   * @param encoding        to be injected into the {@link String} field annotated with {@link DefaultEncoding}
   * @param muleVersion     to be injected into the {@link MuleVersion} field annotated with {@link RuntimeVersion}
   * @param reflectionCache the cache for expensive reflection lookups
   * @throws IllegalModelDefinitionException if there is more than one field annotated with {@link DefaultEncoding} or
   *                                         {@link RuntimeVersion}
   */
  public static void injectFields(Object target, String configName, String encoding, MuleVersion muleVersion,
                                  ReflectionCache reflectionCache) {
    set(getDefaultEncodingFieldSetter(target, reflectionCache), target, encoding);
    set(getRefNameFieldSetter(target, reflectionCache), target, configName);
    set(getFieldSetterForAnnotatedField(target, RuntimeVersion.class, reflectionCache), target, muleVersion);
  }

  /**
   * Introspects the {@code target} object for a field annotated with {@link RefName} or
   * {@link org.mule.sdk.api.annotation.param.RefName}. If found, it injects the {@code configName} value into it.
   * <p>
   * The {@code target} object is expected to have only one field annotated with {@link RefName} or
   * {@link org.mule.sdk.api.annotation.param.RefName} and that field is required to be a String.
   *
   * @param target          object in which the value are going to be set
   * @param configName      the value to be injected
   * @param reflectionCache the cache for expensive reflection lookups
   */
  public static void injectRefName(Object target, String configName, ReflectionCache reflectionCache) {
    set(getRefNameFieldSetter(target, reflectionCache), target, configName);
  }

  /**
   * Returns a {@link FieldSetter} for a field in the {@code target} annotated {@link DefaultEncoding} (if present)
   *
   * @param target          object in which the fields are going to be set
   * @param reflectionCache the cache for expensive reflection lookups
   * @throws IllegalModelDefinitionException} if there is more than one field annotated with {@link DefaultEncoding}
   */
  public static Optional<FieldSetter> getDefaultEncodingFieldSetter(Object target, ReflectionCache reflectionCache) {
    Optional<FieldSetter> legacyDefaultEncodingFieldSetter =
        getFieldSetterForAnnotatedField(target, DefaultEncoding.class, reflectionCache);
    return legacyDefaultEncodingFieldSetter.isPresent() ? legacyDefaultEncodingFieldSetter
        : getFieldSetterForAnnotatedField(target, org.mule.sdk.api.annotation.param.DefaultEncoding.class, reflectionCache);
  }

  /**
   * Returns {@link FieldSetter} for a field in the {@code target} annotated {@link RefName} or
   * {@link org.mule.sdk.api.annotation.param.RefName} if present.
   *
   * @param target          object in which the fields are going to be set
   * @param reflectionCache the cache for expensive reflection lookups
   * @return the {@link FieldSetter}
   * @throws IllegalModelDefinitionException if there is more than one field annotated with {@link RefName} and or
   *                                         {@link org.mule.sdk.api.annotation.param.RefName}
   */
  public static Optional<FieldSetter> getRefNameFieldSetter(Object target, ReflectionCache reflectionCache) {
    Optional<FieldSetter> legacyRefNameFieldSetter =
        getFieldSetterForAnnotatedField(target, RefName.class, reflectionCache);
    Optional<FieldSetter> sdkRefNameFieldSetter =
        getFieldSetterForAnnotatedField(target, org.mule.sdk.api.annotation.param.RefName.class, reflectionCache);
    if (legacyRefNameFieldSetter.isPresent() & sdkRefNameFieldSetter.isPresent()) {
      throw new IllegalModelDefinitionException(format(
                                                       "Class '%s' has 2 fields annotated with '@%s' or '@%s'. Only one field may carry those annotations",
                                                       target.getClass().getName(), RefName.class.getName(),
                                                       org.mule.sdk.api.annotation.param.RefName.class.getName()));
    } else if (legacyRefNameFieldSetter.isPresent()) {
      return legacyRefNameFieldSetter;
    } else if (sdkRefNameFieldSetter.isPresent()) {
      return sdkRefNameFieldSetter;
    } else {
      return empty();
    }
  }

  /**
   * Introspects the {@code target} object for a field of type {@link ComponentLocation}. If found, it injects the
   * {@code componentLocation} value into it.
   * <p>
   * The {@code target} object is expected to have only one field of such type.
   *
   * @param target            object in which the value are going to be set
   * @param componentLocation the value to be injected
   */
  public static void injectComponentLocation(Object target, ComponentLocation componentLocation) {
    injectFieldOfType(target, componentLocation, ComponentLocation.class);
  }

  private static void set(Optional<FieldSetter> setter, Object target, Object value) {
    setter.ifPresent(s -> s.set(target, value));
  }

  /**
   * Introspects a {@link PagingProvider} type and returns their generics.
   *
   * @param pagingProvider {@link PagingProvider} to introspect
   * @return The {@link PagingProvider} generics.
   */
  public static Pair<ResolvableType, ResolvableType> getPagingProviderTypes(ResolvableType pagingProvider) {
    if (!PagingProvider.class.isAssignableFrom(pagingProvider.getRawClass())) {
      throw new IllegalArgumentException("The given OutputType is not a PagingProvider");
    }

    ResolvableType[] generics = pagingProvider.getGenerics();
    ResolvableType connectionType = null;
    ResolvableType returnType = null;

    if (generics.length == 0) {
      for (ResolvableType resolvableType : pagingProvider.getInterfaces()) {
        if (resolvableType.getRawClass().equals(PagingProvider.class)) {
          connectionType = resolvableType.getGeneric(0);
          returnType = resolvableType.getGeneric(1);
        }
      }
    } else {
      connectionType = generics[0];
      returnType = generics[1];
    }
    return new Pair<>(connectionType, returnType);
  }

  /**
   * Introspects a {@link PagingProvider} type and returns their generics.
   *
   * @return The {@link PagingProvider} generics.
   */
  public static Pair<Type, Type> getPagingProviderTypes(Type type) {
    if (!type.isAssignableTo(PagingProvider.class)) {
      throw new IllegalArgumentException("The given OutputType is not a PagingProvider");
    }

    List<Type> interfaceGenerics = type.getSuperTypeGenerics(PagingProvider.class);
    if (interfaceGenerics.size() == 2) {
      return new Pair<>(interfaceGenerics.get(0), interfaceGenerics.get(1));
    } else {
      throw new IllegalStateException("PagingProvider must provide their generics");
    }
  }

  /**
   * Given a {@link ParameterizedModel} iterates over all the parameter groups show in dsl and returns a mapping of parameter name
   * and parameter group name.
   *
   * @param parameterizedModel Model to introspect.
   * @return A map with parameters from Show in DSL parameters
   * @since 4.1.1
   */
  public static Map<String, String> getShowInDslParameters(ParameterizedModel parameterizedModel) {
    HashMap<String, String> showInDslMap = new HashMap<>();

    parameterizedModel.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .forEach(groupModel -> groupModel.getParameterModels()
            .forEach(param -> showInDslMap.put(IntrospectionUtils.getImplementingName(param),
                                               getGroupModelContainerName(groupModel))));

    return showInDslMap;
  }

  /**
   * @return a boolean indicating if the given type should be considered as a {@link AnyType Any type}
   */
  private static boolean isAnyType(Type type) {
    return type.isSameType(Object.class) ||
        type.isSameType(Serializable.class) ||
        type.isAssignableTo(InputStream.class) ||
        type.isAssignableTo(Byte[].class) ||
        type.isAssignableTo(byte[].class) ||
        type.isSameType(CursorProvider.class) ||
        type.isAssignableTo(CursorStreamProvider.class);
  }

  /**
   * @return a boolean indicating if the given type should be considered as a {@link ArrayType array} of {@link AnyType Any type}
   */
  private static boolean isArrayOfAny(Type type) {
    return type.isAssignableTo(CursorIteratorProvider.class);
  }


  /**
   * Sets the given {@code value} into the {@code target}, if it contains a field named {@code fieldName}. Nothing happens if such
   * field doesn't exist.
   *
   * @param target          the object in which the value is to be set
   * @param value           the value to set
   * @param fieldName       the name of the target field
   * @param reflectionCache the {@link ReflectionCache} used to introspect the {@code target}
   * @since 4.4.0
   */
  public static void setValueIntoField(Object target,
                                       Object value,
                                       String fieldName,
                                       ReflectionCache reflectionCache) {
    getField(target.getClass(), fieldName, reflectionCache).ifPresent(field -> setValueIntoField(target, value, field));
  }

  /**
   * Sets the given {@code value} into the {@code target} {@code field}
   *
   * @param target the object in which the value is to be set
   * @param value  the value to set
   * @param field  the field in which the value is to be set
   * @since 4.4.0
   */
  public static void setValueIntoField(Object target, Object value, Field field) {
    FieldSetter setter = new FieldSetter<>(field);
    setter.set(target, value);
  }

  /**
   * Returns, for each parameter group defined on the component model, all the parameters matching with the given filter
   *
   * @param componentModel the component model used to filter the parameters
   * @param filter         the filter to apply to the group model parameters
   * @return The map having the parameter group as key and a set of its parameters matching the filter as value
   */
  public static Map<ParameterGroupModel, Set<ParameterModel>> getFilteredParameters(ComponentModel componentModel,
                                                                                    Predicate<ParameterModel> filter) {
    Map<ParameterGroupModel, Set<ParameterModel>> filteredParameters = new HashMap<>();
    componentModel.getParameterGroupModels().forEach(parameterGroupModel -> {
      parameterGroupModel.getParameterModels().stream()
          .filter(filter)
          .forEach(p -> filteredParameters.computeIfAbsent(parameterGroupModel, k -> new HashSet<>()).add(p));
    });
    return filteredParameters;
  }

  /**
   * Returns the value associated to a parameter with model {@code parameterModel} on the group {@code parameterGroupModel} or
   * {@code defaultValue} if such parameter is not present
   *
   * @param ctx                 the execution where to look for the parameter
   * @param parameterGroupModel the parameter group where the parameter is defined
   * @param parameterModel      the parameter model which value will be returned
   * @param defaultValue        the default value to return in case the parameter is not present
   * @param reflectionCache     the {@link ReflectionCache} used to introspect the parameter value
   * @param <T>                 the returned value's generic type
   * @return the parameter's value or {@code defaultValue}
   */
  public static <T> T getParameterOrDefault(ExecutionContext<? extends ComponentModel> ctx,
                                            ParameterGroupModel parameterGroupModel,
                                            ParameterModel parameterModel, T defaultValue, ReflectionCache reflectionCache) {
    if (!parameterGroupModel.isShowInDsl()) {
      return ctx.getParameterOrDefault(parameterModel.getName(), defaultValue);
    } else {
      try {
        Object container = ctx.getParameterOrDefault(getGroupModelContainerName(parameterGroupModel), defaultValue);
        return (T) getParameterValue(container, parameterModel, reflectionCache);
      } catch (Exception e) {
        return defaultValue;
      }
    }
  }

  /**
   * Returns the value for the given parameter of an object instance
   *
   * @param object          The object where to grab the parameter value from
   * @param parameterModel  The parameter model for the parameter to obtain the value
   * @param reflectionCache the cache for expensive reflection lookups
   * @return The value of the parameter with the given parameter model on the object instance
   * @throws IllegalAccessException if is unavailable to access to the field
   * @throws NoSuchFieldException   if the field doesn't exist in the given object instance
   */
  public static Object getParameterValue(Object object, ParameterModel parameterModel,
                                         ReflectionCache reflectionCache)
      throws IllegalAccessException, NoSuchFieldException {
    Optional<Field> field = getField(object.getClass(), parameterModel, reflectionCache);
    if (field.isPresent()) {
      return getFieldValue(object, field.get().getName(), reflectionCache);
    }
    throw new NoSuchFieldException();
  }

  /**
   * Given a Source object, fetches the Config field from the object if any.
   *
   * @param object the source object
   * @return an Optional containing the config field if any
   * @since 4.5
   */
  public static Optional<Field> fetchConfigFieldFromSourceObject(Object object) {
    return fetchFieldFromSourceObject(object, Config.class, org.mule.sdk.api.annotation.param.Config.class);
  }

  /**
   * Given a Source object, fetches the Connection field from the object if any.
   *
   * @param object the source object
   * @return an Optional containing the connection field if any
   * @since 4.5
   */
  public static Optional<Field> fetchConnectionFieldFromSourceObject(Object object) {
    return fetchFieldFromSourceObject(object, Connection.class, org.mule.sdk.api.annotation.param.Connection.class);
  }

  private static Optional<Field> fetchFieldFromSourceObject(Object object, Class<? extends Annotation>... annotations) {
    Set<Field> fields = new HashSet<>();
    for (Class<? extends Annotation> annotation : annotations) {
      fields.addAll(getAllFields(object.getClass(), withAnnotation(annotation)));
    }
    if (fields.isEmpty()) {
      return empty();
    }

    if (fields.size() > 1) {
      // TODO: MULE-9220 Move this to a syntax validator
      throw new IllegalModelDefinitionException(
                                                format("Message Source defined on class '%s' has more than one field annotated with '@%s'. "
                                                    + "Only one field in the class can bare such annotation",
                                                       object.getClass().getName(),
                                                       annotations[0].getClass().getSimpleName()));
    }

    return of(fields.iterator().next());
  }

  /**
   * Allows the cleanup of the shaded Spring {@link org.springframework.util.ConcurrentReferenceHashMap} caches.
   */
  public static void resetCommonCaches()
      throws Exception {
    ByteBuddySpringCachesManager.clearCaches();
  }
}
