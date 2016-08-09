/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isObjectType;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Ignore;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.ResolvableType;

/**
 * Set of utility operations to get insights about objects and their components
 *
 * @since 3.7.0
 */
public final class IntrospectionUtils {

  private static final String CONFIGURATION = "configuration";
  private static final String OPERATION = "operation";
  private static final String CONNECTION_PROVIDER = "connection provider";
  private static final String SOURCE = "source";

  private IntrospectionUtils() {}

  /**
   * Returns a {@link MetadataType} representing the given {@link Class} type.
   *
   * @param type the {@link Class} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   */
  public static MetadataType getMetadataType(Class<?> type, ClassTypeLoader typeLoader) {
    return typeLoader.load(ResolvableType.forClass(type).getType());
  }

  /**
   * Returns a {@link MetadataType} representing the given {@link Method}'s return type. If the {@code method} returns an
   * {@link OperationResult}, then it returns the type of the {@code Output} generic. If the {@link OperationResult} type is being
   * used in its raw form, then an {@link AnyType} will be returned.
   *
   * @param method the {@link Method} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType getMethodReturnType(Method method, ClassTypeLoader typeLoader) {
    ResolvableType methodType = unwrapInterceptingCallback(method);

    Type type = methodType.getType();
    if (methodType.getRawClass().equals(OperationResult.class)) {
      ResolvableType genericType = methodType.getGenerics()[0];
      if (genericType.getRawClass() != null) {
        type = genericType.getType();
      } else {
        type = null;
      }
    }

    return type != null ? typeLoader.load(type) : typeBuilder().anyType().build();
  }

  /**
   * Returns a {@link MetadataType} representing the {@link OperationResult#getAttributes()} that will be set after executing the
   * given {@code method}.
   * <p>
   * If the {@code method} returns a {@link OperationResult}, then it returns the type of the {@code Attributes} generic. In any
   * other case (including raw uses of {@link OperationResult}) it will return a {@link NullType}
   *
   * @param method the {@link Method} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType getMethodReturnAttributesType(Method method, ClassTypeLoader typeLoader) {
    Type type = null;

    ResolvableType methodType = unwrapInterceptingCallback(method);

    if (methodType.getRawClass().equals(OperationResult.class)) {
      ResolvableType genericType = methodType.getGenerics()[1];
      if (genericType.getRawClass() != null) {
        type = genericType.getType();
      }
    }

    return type != null ? typeLoader.load(type) : typeBuilder().nullType().build();
  }

  private static ResolvableType unwrapInterceptingCallback(Method method) {
    ResolvableType methodType = getMethodResolvableType(method);
    if (InterceptingCallback.class.isAssignableFrom(methodType.getRawClass())) {
      ResolvableType genericType = methodType.getGenerics()[0];
      if (genericType.getRawClass() != null) {
        methodType = genericType;
      }
    }
    return methodType;
  }

  private static ResolvableType getMethodResolvableType(Method method) {
    checkArgument(method != null, "Can't introspect a null method");
    return ResolvableType.forMethodReturnType(method);
  }

  private static BaseTypeBuilder<?> typeBuilder() {
    return BaseTypeBuilder.create(JAVA);
  }

  /**
   * Returns an array of {@link MetadataType} representing each of the given {@link Method}'s argument types.
   *
   * @param method a not {@code null} {@link Method}
   * @param typeLoader a {@link ClassTypeLoader} to be used to create the returned {@link MetadataType}s
   * @return an array of {@link MetadataType} matching the method's arguments. If the method doesn't take any, then the array will
   *         be empty
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType[] getMethodArgumentTypes(Method method, ClassTypeLoader typeLoader) {
    checkArgument(method != null, "Can't introspect a null method");
    Class<?>[] parameters = method.getParameterTypes();
    if (ArrayUtils.isEmpty(parameters)) {
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
   * @param field a not {@code null} {@link Field}
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType} matching the field's type
   * @throws IllegalArgumentException if field is {@code null}
   */
  public static MetadataType getFieldMetadataType(Field field, ClassTypeLoader typeLoader) {
    checkArgument(field != null, "Can't introspect a null field");
    return typeLoader.load(ResolvableType.forField(field).getType());
  }

  public static Field getField(Class<?> clazz, ParameterModel parameterModel) {
    return getField(clazz, getMemberName(parameterModel, parameterModel.getName()));
  }

  public static Field getField(Class<?> clazz, ParameterDeclaration parameterDeclaration) {
    return getField(clazz, MuleExtensionAnnotationParser.getMemberName(parameterDeclaration, parameterDeclaration.getName()));
  }

  public static Field getField(Class<?> clazz, String name) {
    Collection<Field> candidates = getAllFields(clazz, withName(name));
    return CollectionUtils.isEmpty(candidates) ? null : candidates.iterator().next();
  }

  public static Field getFieldByAlias(Class<?> clazz, String alias) {
    Collection<Field> candidates = getAllFields(clazz, withAnnotation(Alias.class));
    return candidates.stream().filter(f -> alias.equals(f.getAnnotation(Alias.class).value())).findFirst()
        .orElseGet(() -> getField(clazz, alias));
  }

  public static String getMemberName(EnrichableModel enrichableModel, String defaultName) {
    return enrichableModel.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField().getName())
        .orElse(defaultName);
  }

  public static boolean hasDefaultConstructor(Class<?> clazz) {
    return ClassUtils.getConstructor(clazz, new Class[] {}) != null;
  }

  public static List<Class<?>> getInterfaceGenerics(final Class<?> type, final Class<?> implementedInterface) {
    ResolvableType interfaceType = null;
    Class<?> searchClass = type;

    while (!Object.class.equals(searchClass)) {
      for (ResolvableType iType : ResolvableType.forClass(searchClass).getInterfaces()) {
        if (implementedInterface.isAssignableFrom(iType.getRawClass())) {
          interfaceType = iType;
          break;
        }
      }

      if (interfaceType != null) {
        break;
      } else {
        searchClass = searchClass.getSuperclass();
      }
    }

    if (interfaceType == null) {
      throw new IllegalArgumentException(format("Class '%s' does not implement the '%s' interface", type.getName(),
                                                implementedInterface.getName()));
    }

    List<? super Class<?>> generics = toRawClasses(interfaceType.getGenerics());
    if (generics.stream().anyMatch(c -> c == null)) {
      return findGenericsInSuperHierarchy(type);
    }

    return (List<Class<?>>) generics;
  }

  public static List<Class<?>> findGenericsInSuperHierarchy(final Class<?> type) {
    if (Object.class.equals(type)) {
      return ImmutableList.of();
    }

    Class<?> superClass = type.getSuperclass();

    List<Type> generics = getSuperClassGenerics(type, superClass);

    if (CollectionUtils.isEmpty(generics) && !Object.class.equals(superClass)) {
      return findGenericsInSuperHierarchy(superClass);
    }

    return (List) generics;
  }

  private static List<Class<?>> toRawClasses(ResolvableType... types) {
    return stream(types).map(ResolvableType::getRawClass).collect(toList());
  }

  public static List<Type> getSuperClassGenerics(Class<?> type, Class<?> superClass) {
    Class<?> searchClass = type;

    checkArgument(searchClass.getSuperclass().equals(superClass),
                  format("Class '%s' does not extend the '%s' class", type.getName(), superClass.getName()));

    while (!Object.class.equals(searchClass)) {
      if (searchClass.getSuperclass().equals(superClass)) {
        Type superType = searchClass.getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
          return stream(((ParameterizedType) superType).getActualTypeArguments()).collect(toList());
        }
      }
      searchClass = searchClass.getSuperclass();
    }
    return new LinkedList<>();
  }

  public static void checkInstantiable(Class<?> declaringClass) {
    checkInstantiable(declaringClass, true);
  }

  public static void checkInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor) {
    if (!isInstantiable(declaringClass, requireDefaultConstructor)) {
      throw new IllegalArgumentException(format("Class %s cannot be instantiated.", declaringClass));
    }
  }

  public static boolean isInstantiable(Class<?> declaringClass) {
    return isInstantiable(declaringClass, true);
  }

  public static boolean isInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor) {
    return declaringClass != null && (!requireDefaultConstructor || hasDefaultConstructor(declaringClass))
        && !declaringClass.isInterface() && !Modifier.isAbstract(declaringClass.getModifiers());
  }

  public static boolean isRequired(AccessibleObject object) {
    return object.getAnnotation(Optional.class) == null;
  }

  public static boolean isRequired(ParameterModel parameterModel, boolean forceOptional) {
    return !forceOptional && parameterModel.isRequired();
  }

  public static boolean isVoid(Method method) {
    return isVoid(method.getReturnType());
  }

  public static boolean isVoid(ComponentModel componentModel) {
    return componentModel.getOutput().getType() instanceof NullType;
  }

  private static boolean isVoid(Class<?> type) {
    return type.equals(void.class) || type.equals(Void.class);
  }

  public static Collection<Field> getParameterFields(Class<?> extensionType) {
    return getAnnotatedFields(extensionType, Parameter.class);
  }

  public static Collection<Field> getParameterGroupFields(Class<?> extensionType) {
    return ImmutableList.copyOf(getAnnotatedFields(extensionType, ParameterGroup.class));
  }

  public static Collection<Method> getOperationMethods(Class<?> declaringClass) {
    return getAllMethods(declaringClass, withModifier(Modifier.PUBLIC), Predicates.not(withAnnotation(Ignore.class)));
  }

  public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return getDescendingHierarchy(clazz).stream().flatMap(type -> stream(type.getDeclaredFields()))
        .filter(field -> field.getAnnotation(annotationType) != null).collect(new ImmutableListCollector<>());
  }

  public static List<Field> getFields(Class<?> clazz) {
    return getDescendingHierarchy(clazz).stream().flatMap(type -> stream(type.getDeclaredFields()))
        .collect(new ImmutableListCollector<>());
  }

  private static List<Class<?>> getDescendingHierarchy(Class<?> type) {
    List<Class<?>> types = new LinkedList<>();
    types.add(type);
    for (type = type.getSuperclass(); type != null && !Object.class.equals(type); type = type.getSuperclass()) {
      types.add(0, type);
    }

    return ImmutableList.copyOf(types);
  }

  public static Collection<Field> getExposedFields(Class<?> extensionType) {
    Collection<Field> allFields = getAnnotatedFields(extensionType, Parameter.class);
    if (!allFields.isEmpty()) {
      return allFields;
    }
    return getFieldsWithGetterAndSetters(extensionType);
  }

  public static Set<Field> getFieldsWithGetterAndSetters(Class<?> extensionType) {
    try {
      PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(extensionType).getPropertyDescriptors();
      return stream(propertyDescriptors).map(p -> getField(extensionType, p.getName())).filter(field -> field != null)
          .collect(toSet());
    } catch (IntrospectionException e) {
      throw new IllegalModelDefinitionException("Could not introspect POJO: " + extensionType.getName(), e);
    }
  }

  public static ExpressionSupport getExpressionSupport(AnnotatedElement object) {
    return getExpressionSupport(object.getAnnotation(Expression.class));
  }

  public static ExpressionSupport getExpressionSupport(Expression expressionAnnotation) {
    return expressionAnnotation != null ? expressionAnnotation.value() : SUPPORTED;
  }

  public static String getAliasName(Class<?> type) {
    return getAliasName(type.getSimpleName(), type.getAnnotation(Alias.class));
  }

  public static String getAliasName(String defaultName, Alias aliasAnnotation) {
    String alias = aliasAnnotation != null ? aliasAnnotation.value() : null;
    return StringUtils.isEmpty(alias) ? defaultName : alias;
  }

  public static String getSourceName(Class<? extends Source> sourceType) {
    Alias alias = sourceType.getAnnotation(Alias.class);
    if (alias != null) {
      return alias.value();
    }

    return sourceType.getSimpleName();
  }

  public static java.util.Optional<ParameterModel> getContentParameter(ComponentModel component) {
    return component.getParameterModels().stream().filter(p -> p.getModelProperty(MetadataContentModelProperty.class).isPresent())
        .findFirst();
  }

  public static List<ParameterModel> getMetadataKeyParts(ComponentModel component) {
    return component.getParameterModels().stream().filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .collect(toList());
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
  public static Set<Class<?>> getParameterClasses(ExtensionModel extensionModel) {
    ImmutableSet.Builder<Class<?>> parameterClasses = ImmutableSet.builder();
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterModel model) {
        parameterClasses.add(getType(model.getType()));
      }
    }.walk(extensionModel);

    return parameterClasses.build();
  }

  /**
   * Given a {@link Set} of Annotation classes and a {@link MetadataType} that describes a component parameter, indicates if the
   * parameter is considered as a multilevel {@link MetadataKeyId}
   *
   * @param annotations of the parameter
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
   * @param annotations of the component parameter
   * @param parameterType of the component parameter
   * @return a boolean indicating if the parameter is considered as a parameter container
   */
  public static boolean isParameterContainer(Set<Class<? extends Annotation>> annotations, MetadataType parameterType) {
    return (annotations.contains(ParameterGroup.class) || isMultiLevelMetadataKeyId(annotations, parameterType));
  }

  public static String getComponentModelTypeName(Object component) {
    if (component instanceof OperationModel) {
      return OPERATION;
    } else if (component instanceof ConfigurationModel) {
      return CONFIGURATION;
    } else if (component instanceof ConnectionProviderModel) {
      return CONNECTION_PROVIDER;
    } else if (component instanceof SourceModel) {
      return SOURCE;
    }

    throw new IllegalArgumentException(format("Component '%s' is not an instance of any known model type [%s, %s, %s, %s]",
                                              component.toString(), CONFIGURATION, CONNECTION_PROVIDER, OPERATION, SOURCE));
  }

  public static String getModelName(Object model) {
    if (model instanceof Named) {
      return ((Named) model).getName();
    }

    throw new IllegalArgumentException(format("Model '%s' is not a named type"));
  }
}
