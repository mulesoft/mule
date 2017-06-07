/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isEnum;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isObjectType;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableList;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllSuperTypes;
import static org.reflections.ReflectionUtils.withName;
import static org.springframework.core.ResolvableType.forMethodReturnType;
import static org.springframework.core.ResolvableType.forType;
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
import org.mule.metadata.message.MessageMetadataTypeBuilder;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.ParameterResolverTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.internal.property.LiteralModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.TypedValueTypeModelProperty;

import com.google.common.collect.ImmutableList;
import org.springframework.core.ResolvableType;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Set of utility operations to get insights about objects and their components
 *
 * @since 3.7.0
 */
public final class IntrospectionUtils {

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

  /**
   * Transforms a {@link MetadataType} and generates the correspondent {@link DataType}
   *
   * @param metadataType to introspect a create a {@link DataType} from it.
   * @return a {@link DataType} based on the given {@link MetadataType}
   */
  public static DataType toDataType(MetadataType metadataType) {
    Class<?> type = getType(metadataType);
    Reference<DataType> dataType = new Reference<>();

    metadataType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        dataType.set(DataType.fromType(type));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Class itemClass = getType(arrayType.getType());
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
                    return getType(restriction);
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
   * Returns a {@link MetadataType} representing the given {@link Method}'s return type. If the {@code method} returns an
   * {@link Result}, then it returns the type of the first generic. If the {@link Result} type is being used in its raw
   * form, then an {@link AnyType} will be returned.
   * <p>
   * If the {@code method} returns a collection of {@link Result} instances, then it will
   * return an {@link ArrayType} which inner value represent a {@link Message} which payload
   * and attributes matches the types of the Result generics.
   *
   * @param method     the {@link Method} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType getMethodReturnType(Method method, ClassTypeLoader typeLoader) {
    return getReturnType(getMethodType(method), typeLoader);
  }

  /**
   * Returns the {@link MetadataType} for a source's output.
   * <p>
   * If the {@code type} is a collection of {@link Result} instances, then it will
   * return an {@link ArrayType} which inner value represent a {@link Message} which payload
   * and attributes matches the types of the Result generics.
   *
   * @param returnType the source output type
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   */
  public static MetadataType getSourceReturnType(Type returnType, ClassTypeLoader typeLoader) {
    return getReturnType(forType(returnType), typeLoader);
  }

  private static MetadataType getReturnType(ResolvableType returnType, ClassTypeLoader typeLoader) {
    Type type = returnType.getType();
    final Class<?> rawClass = returnType.getRawClass();

    if (rawClass.equals(Result.class)) {
      ResolvableType genericType = returnType.getGenerics()[0];
      if (genericType.getRawClass() != null) {
        type = genericType.getType();
      } else {
        type = null;
      }
    }

    if (isPagingProvider(returnType)) {
      ResolvableType itemType = returnType.getGenerics()[1];
      if (Result.class.equals(itemType.getRawClass())) {
        return returnListOfMessagesType(returnType, typeLoader, itemType);
      } else {
        return typeBuilder().arrayType().id(rawClass.getName()).of(typeLoader.load(itemType.getType())).build();
      }
    }

    if (ParameterResolver.class.isAssignableFrom(rawClass) ||
        TypedValue.class.isAssignableFrom(rawClass) ||
        Literal.class.isAssignableFrom(rawClass)) {
      type = returnType.getGenerics()[0].getType();
    }

    if (isCollection(returnType)) {
      ResolvableType itemType = returnType.getGenerics()[0];
      if (Result.class.equals(itemType.getRawClass())) {
        return returnListOfMessagesType(returnType, typeLoader, itemType);
      }
    }

    return type != null ? typeLoader.load(type) : typeBuilder().anyType().build();
  }

  private static MetadataType returnListOfMessagesType(ResolvableType returnType, ClassTypeLoader typeLoader,
                                                       ResolvableType itemType) {
    ResolvableType genericType = itemType.getGenerics()[0];

    Class<?> rawClass = genericType.getRawClass();

    if (rawClass != null && TypedValue.class.isAssignableFrom(rawClass)) {
      genericType = genericType.getGenerics()[0];
    }

    MetadataType outputType = rawClass != null
        ? typeLoader.load(genericType.getType())
        : typeBuilder().anyType().build();

    genericType = itemType.getGenerics()[1];
    MetadataType attributesType = rawClass != null
        ? typeLoader.load(genericType.getType())
        : typeBuilder().voidType().build();

    return typeBuilder().arrayType().id(returnType.getRawClass().getName())
        .of(new MessageMetadataTypeBuilder().payload(outputType).attributes(attributesType).build())
        .build();
  }

  private static boolean isCollection(ResolvableType type) {
    return Collection.class.isAssignableFrom(type.getRawClass());
  }

  /**
   * Returns a {@link MetadataType} representing the {@link Result#getAttributes()} that will be set after executing the given
   * {@code method}.
   * <p>
   * If the {@code method} returns a {@link Result}, then it returns the type of the {@code Attributes} generic. In any other case
   * (including raw uses of {@link Result}) it will return a {@link VoidType}
   * <p>
   * If the {@code method} returns a collection or a {@link PagingProvider} of {@link Result}, then this will return {@link VoidType} since the messages in the
   * main output already contain an attributes for each item.
   *
   * @param method     the {@link Method} being introspected
   * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
   * @return a {@link MetadataType}
   * @throws IllegalArgumentException is method is {@code null}
   */
  public static MetadataType getMethodReturnAttributesType(Method method, ClassTypeLoader typeLoader) {
    ResolvableType outputType = getMethodType(method);
    Type type = null;

    if (Result.class.equals(outputType.getRawClass())) {
      ResolvableType genericType = outputType.getGenerics()[1];
      if (genericType.getRawClass() != null) {
        type = genericType.getType();
      }
    }

    if (isPagingProvider(outputType)) {
      ResolvableType itemType = outputType.getGenerics()[1];
      if (Result.class.equals(itemType.getRawClass())) {
        type = null;
      }
    }

    if (isCollection(outputType)) {
      ResolvableType itemType = outputType.getGenerics()[0];
      if (Result.class.equals(itemType.getRawClass())) {
        type = null;
      }
    }

    return type != null ? typeLoader.load(type) : typeBuilder().voidType().build();
  }

  public static List<MetadataType> getGenerics(Type type, ClassTypeLoader typeLoader) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type[] generics = parameterizedType.getActualTypeArguments();
      return Stream.of(generics).map(typeLoader::load).collect(toList());
    }

    return new LinkedList<>();
  }

  public static ResolvableType getMethodType(Method method) {
    checkArgument(method != null, "Can't introspect a null method");
    return forMethodReturnType(method);
  }

  public static ResolvableType unwrapGenericFromClass(Class<?> clazz, ResolvableType type, int genericIndex) {
    if (!isEmpty(type.getGenerics())) {
      ResolvableType genericType = type.getGenerics()[genericIndex];
      if (genericType.getRawClass() != null) {
        type = genericType;
      }
    } else {
      if (clazz.isAssignableFrom(type.getRawClass().getSuperclass())) {
        return unwrapGenericFromClass(clazz, type.getSuperType(), genericIndex);
      } else {
        ResolvableType interfaceType = stream(type.getInterfaces())
            .filter(i -> clazz.isAssignableFrom(i.getRawClass()))
            .findFirst()
            .orElse(forType(Object.class));

        return unwrapGenericFromClass(clazz, interfaceType, genericIndex);
      }

    }
    return type;
  }

  /**
   * Determines if the given {@code type} implements any of the lifecycle
   * annotations
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

  private static boolean isPagingProvider(ResolvableType type) {
    return PagingProvider.class.isAssignableFrom(type.getRawClass());
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
   * be empty
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

  public static Optional<Field> getFieldByNameOrAlias(Class<?> clazz, String nameOrAlias) {
    Optional<Field> field = getField(clazz, nameOrAlias);
    if (!field.isPresent()) {
      field = getAllFields(clazz, f -> getAlias(f).equals(nameOrAlias)).stream().findFirst();
    }

    return field;
  }

  public static Optional<Field> getField(Class<?> clazz, ParameterModel parameterModel) {
    return getField(clazz, getMemberName(parameterModel, parameterModel.getName()));
  }

  public static Optional<Field> getField(Class<?> clazz, ParameterDeclaration parameterDeclaration) {
    return getField(clazz, MuleExtensionAnnotationParser.getMemberName(parameterDeclaration, parameterDeclaration.getName()));
  }

  public static Optional<Field> getField(Class<?> clazz, String name) {
    Collection<Field> candidates = getAllFields(clazz, withName(name));
    return isEmpty(candidates) ? Optional.empty() : Optional.of(candidates.iterator().next());
  }

  /**
   * Resolves and returns the field value of an object instance
   *
   * @param object    The object where grab the field value
   * @param fieldName The name of the field to obtain the value
   * @return The value of the field with the given fieldName and object instance
   * @throws IllegalAccessException if is unavailable to access to the field
   * @throws NoSuchFieldException   if the field doesn't exist in the given object instance
   */
  public static Object getFieldValue(Object object, String fieldName) throws IllegalAccessException, NoSuchFieldException {
    final Optional<Field> fieldOptional = getField(object.getClass(), fieldName);
    if (fieldOptional.isPresent()) {
      final Field field = fieldOptional.get();
      field.setAccessible(true);
      return field.get(object);
    } else {
      throw new NoSuchFieldException();
    }
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

    if (isEmpty(generics) && !Object.class.equals(superClass)) {
      return findGenericsInSuperHierarchy(superClass);
    }

    return (List) generics;
  }

  private static List<Class<?>> toRawClasses(ResolvableType... types) {
    return stream(types).map(ResolvableType::getRawClass).collect(toList());
  }

  public static List<Type> getSuperClassGenerics(Class<?> type, Class<?> superClass) {
    if (!superClass.isAssignableFrom(type)) {
      throw new IllegalArgumentException(
                                         format("Class '%s' does not extend the '%s' class", type.getName(),
                                                superClass.getName()));
    }

    Class<?> searchClass = type;
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

  public static boolean isInstantiable(MetadataType type) {
    return isInstantiable(getType(type));
  }

  public static boolean isInstantiable(Class<?> declaringClass) {
    return isInstantiable(declaringClass, true);
  }

  public static boolean isInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor) {
    return declaringClass != null && (!requireDefaultConstructor || hasDefaultConstructor(declaringClass))
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
    return object.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) == null;
  }

  public static boolean isRequired(ParameterModel parameterModel, boolean forceOptional) {
    return !forceOptional && parameterModel.isRequired();
  }

  public static boolean isVoid(Method method) {
    return isVoid(method.getReturnType());
  }

  public static boolean isVoid(ComponentModel componentModel) {
    return componentModel.getOutput().getType() instanceof VoidType;
  }

  private static boolean isVoid(Class<?> type) {
    return type.equals(void.class) || type.equals(Void.class);
  }

  public static Collection<Method> getOperationMethods(Class<?> declaringClass) {
    return getMethodsStream(declaringClass)
        .filter(method -> !method.isAnnotationPresent(Ignore.class) && !isLifecycleMethod(method))
        .collect(toCollection(LinkedHashSet::new));
  }

  private static boolean isLifecycleMethod(Method method) {
    return isLifecycleMethod(method, Initialisable.class, "initialise")
        || isLifecycleMethod(method, Startable.class, "start")
        || isLifecycleMethod(method, Stoppable.class, "stop")
        || isLifecycleMethod(method, Disposable.class, "dispose");
  }

  private static boolean isLifecycleMethod(Method method, Class<?> lifecycleClass, String lifecycleMethodName) {
    return lifecycleClass.isAssignableFrom(method.getDeclaringClass()) && method.getName().equals(lifecycleMethodName);
  }

  /**
   * Returns all the methods in the {@code declaringClass} which are annotated with {@code annotationType}, including
   * those declared in super classes.
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
      methodStream = getAllSuperTypes(declaringClass).stream()
          .filter(type -> !type.isInterface())
          .flatMap(type -> Stream.of(type.getDeclaredMethods()));
    } else {
      methodStream = Stream.of(declaringClass.getDeclaredMethods());
    }

    return methodStream.filter(method -> isPublic(method.getModifiers()));
  }


  public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return getDescendingHierarchy(clazz).stream().flatMap(type -> stream(type.getDeclaredFields()))
        .filter(field -> field.getAnnotation(annotationType) != null).collect(toImmutableList());
  }

  public static List<Field> getFields(Class<?> clazz) {
    try {
      return getDescendingHierarchy(clazz).stream().flatMap(type -> stream(type.getDeclaredFields())).collect(toImmutableList());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the {@link Alias} name of the given {@code element}. If the element doesn't have an alias, then the default name is
   * return
   *
   * @param element an annotated member
   * @param <T>     the generic type of the element
   * @return an alias name
   */
  public static <T extends AnnotatedElement & Member> String getAlias(T element) {
    Alias alias = element.getAnnotation(Alias.class);
    return alias != null ? alias.value() : element.getName();
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
    return getFieldsWithGetters(extensionType);
  }

  public static Set<Field> getFieldsWithGetters(Class<?> extensionType) {
    return getPropertyDescriptors(extensionType).stream().filter(p -> p.getReadMethod() != null)
        .map(p -> getField(extensionType, p.getName())).filter(Optional::isPresent).map(Optional::get)
        .collect(toSet());
  }

  private static List<PropertyDescriptor> getPropertyDescriptors(Class<?> extensionType) {
    try {
      PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(extensionType).getPropertyDescriptors();
      return Arrays.asList(propertyDescriptors);
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

  public static String getSourceName(Class<? extends Source> sourceType) {
    Alias alias = sourceType.getAnnotation(Alias.class);
    if (alias != null) {
      return alias.value();
    }

    return sourceType.getSimpleName();
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

        if (!relativeClasses.contains(getType(objectType))) {


          Optional<ClassInformationAnnotation> classInformation = objectType.getAnnotation(ClassInformationAnnotation.class);
          if (classInformation.isPresent()) {
            classInformation.get().getGenericTypes()
                .forEach(generic -> relativeClasses.add(loadClass(generic, extensionClassLoader)));
          }

          relativeClasses.add(getType(objectType));
          objectType.getFields().stream().forEach(objectFieldType -> objectFieldType.accept(this));
          objectType.getOpenRestriction().ifPresent(t -> t.accept(this));
        }
      }

      @Override
      public void visitString(StringType stringType) {
        if (stringType.getMetadataFormat() == JAVA && isEnum(stringType)) {
          relativeClasses.add(getType(stringType));
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
    return (annotations.contains(ParameterGroup.class) || isMultiLevelMetadataKeyId(annotations, parameterType));
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

  public static boolean isParameterResolver(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(modelProperty -> modelProperty instanceof ParameterResolverTypeModelProperty);
  }

  public static boolean isParameterResolver(MetadataType metadataType) {
    return metadataType.getAnnotation(ParameterResolverTypeAnnotation.class).isPresent();
  }

  public static boolean isLiteral(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(modelProperty -> modelProperty instanceof LiteralModelProperty);
  }

  public static boolean isLiteral(MetadataType metadataType) {
    return metadataType.getAnnotation(LiteralTypeAnnotation.class).isPresent();
  }

  public static boolean isTypedValue(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(modelProperty -> modelProperty instanceof TypedValueTypeModelProperty);
  }

  public static boolean isTypedValue(MetadataType metadataType) {
    return metadataType.getAnnotation(TypedValueTypeAnnotation.class).isPresent();
  }

}
