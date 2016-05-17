/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.JavaTypeLoader.JAVA;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getMemberName;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Ignore;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParametrizedModel;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.runtime.source.Source;

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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.ResolvableType;

/**
 * Set of utility operations to get insights about objects and their components
 *
 * @since 3.7.0
 */
public final class IntrospectionUtils
{

    private IntrospectionUtils()
    {
    }

    /**
     * Returns a {@link MetadataType} representing the given {@link Class} type.
     *
     * @param type       the {@link Class} being introspected
     * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
     * @return a {@link MetadataType}
     */
    public static MetadataType getMetadataType(Class<?> type, ClassTypeLoader typeLoader)
    {
        return typeLoader.load(ResolvableType.forClass(type).getType());
    }

    /**
     * Returns a {@link MetadataType} representing the given {@link Method}'s return type.
     * If the {@code method} returns a {@link MuleMessage}, then it returns the type
     * of the {@code Payload} generic. If the {@link MuleMessage} type is being used
     * in its raw form, then an {@link AnyType} will be returned.
     *
     * @param method     the {@link Method} being introspected
     * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
     * @return a {@link MetadataType}
     * @throws IllegalArgumentException is method is {@code null}
     */
    public static MetadataType getMethodReturnType(Method method, ClassTypeLoader typeLoader)
    {
        return getMethodType(method, typeLoader, 0, () -> {
            ResolvableType methodType = getMethodResolvableType(method);
            return methodType.getRawClass().equals(MuleMessage.class)
                   ? typeBuilder().anyType().build()
                   : typeLoader.load(methodType.getType());
        });
    }

    /**
     * Returns a {@link MetadataType} representing the {@link MuleMessage#getAttributes()}
     * that will be set after executing the given {@code method}.
     * <p>
     * If the {@code method} returns a {@link MuleMessage}, then it returns the type
     * of the {@code Attributes} generic. In any other case
     * (including raw uses of {@link MuleMessage}) it will return a {@link NullType}
     *
     * @param method     the {@link Method} being introspected
     * @param typeLoader a {@link ClassTypeLoader} used to create the {@link MetadataType}
     * @return a {@link MetadataType}
     * @throws IllegalArgumentException is method is {@code null}
     */
    public static MetadataType getMethodReturnAttributesType(Method method, ClassTypeLoader typeLoader)
    {
        return getMethodType(method, typeLoader, 1, () -> typeBuilder().nullType().build());
    }

    private static MetadataType getMethodType(Method method,
                                              ClassTypeLoader typeLoader,
                                              int genericIndex,
                                              Supplier<MetadataType> fallbackSupplier)
    {
        ResolvableType methodType = getMethodResolvableType(method);
        Type type = null;
        if (methodType.getRawClass().equals(MuleMessage.class))
        {
            ResolvableType genericType = methodType.getGenerics()[genericIndex];
            if (genericType.getRawClass() != null)
            {
                type = genericType.getType();
            }
        }

        return type != null ? typeLoader.load(type) : fallbackSupplier.get();
    }

    private static ResolvableType getMethodResolvableType(Method method)
    {
        checkArgument(method != null, "Can't introspect a null method");
        return ResolvableType.forMethodReturnType(method);
    }

    private static BaseTypeBuilder<?> typeBuilder()
    {
        return BaseTypeBuilder.create(JAVA);
    }

    /**
     * Returns an array of {@link MetadataType} representing each of the given {@link Method}'s argument
     * types.
     *
     * @param method     a not {@code null} {@link Method}
     * @param typeLoader a {@link ClassTypeLoader} to be used to create the returned {@link MetadataType}s
     * @return an array of {@link MetadataType} matching
     * the method's arguments. If the method doesn't take any, then the array will be empty
     * @throws IllegalArgumentException is method is {@code null}
     */
    public static MetadataType[] getMethodArgumentTypes(Method method, ClassTypeLoader typeLoader)
    {
        checkArgument(method != null, "Can't introspect a null method");
        Class<?>[] parameters = method.getParameterTypes();
        if (ArrayUtils.isEmpty(parameters))
        {
            return new MetadataType[] {};
        }

        MetadataType[] types = new MetadataType[parameters.length];
        for (int i = 0; i < parameters.length; i++)
        {
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
    public static MetadataType getFieldMetadataType(Field field, ClassTypeLoader typeLoader)
    {
        checkArgument(field != null, "Can't introspect a null field");
        return typeLoader.load(ResolvableType.forField(field).getType());
    }

    public static Field getField(Class<?> clazz, ParameterModel parameterModel)
    {
        return getField(clazz, getMemberName(parameterModel, parameterModel.getName()), getType(parameterModel.getType()));
    }

    public static Field getField(Class<?> clazz, ParameterDeclaration parameterDeclaration)
    {
        return getField(clazz, getMemberName(parameterDeclaration, parameterDeclaration.getName()), getType(parameterDeclaration.getType()));
    }

    public static Field getField(Class<?> clazz, String name, Class<?> type)
    {
        Collection<Field> candidates = getAllFields(clazz, withName(name), withTypeAssignableTo(type));
        return CollectionUtils.isEmpty(candidates) ? null : candidates.iterator().next();
    }

    public static Field getFieldByAlias(Class<?> clazz, String alias, Class<?> type)
    {
        Collection<Field> candidates = getAllFields(clazz, withAnnotation(Alias.class), withTypeAssignableTo(type));
        return candidates.stream()
                .filter(f -> alias.equals(f.getAnnotation(Alias.class).value()))
                .findFirst()
                .orElseGet(() -> getField(clazz, alias, type));
    }

    public static boolean hasDefaultConstructor(Class<?> clazz)
    {
        return ClassUtils.getConstructor(clazz, new Class[] {}) != null;
    }

    public static List<Class<?>> getInterfaceGenerics(Class<?> type, Class<?> implementedInterface)
    {
        ResolvableType interfaceType = null;
        Class<?> searchClass = type;

        while (!Object.class.equals(searchClass))
        {
            for (ResolvableType iType : ResolvableType.forClass(searchClass).getInterfaces())
            {
                if (iType.getRawClass().equals(implementedInterface))
                {
                    interfaceType = iType;
                    break;
                }
            }

            if (interfaceType != null)
            {
                break;
            }
            else
            {
                searchClass = searchClass.getSuperclass();
            }
        }

        if (interfaceType == null)
        {
            throw new IllegalArgumentException(String.format("Class '%s' does not implement the '%s' interface", type.getName(), implementedInterface.getName()));
        }

        List<? super Class<?>> generics = toRawClasses(interfaceType.getGenerics());
        if (generics.stream().anyMatch(c -> c == null))
        {
            generics = getSuperClassGenerics(type, searchClass);
        }

        return (List<Class<?>>) generics;
    }

    private static List<Class<?>> toRawClasses(ResolvableType... types)
    {
        return stream(types).map(ResolvableType::getRawClass).collect(toList());
    }

    public static List<Type> getSuperClassGenerics(Class<?> type, Class<?> superClass)
    {
        Class<?> searchClass = type;

        checkArgument(searchClass.getSuperclass().equals(superClass), String.format("Class '%s' does not extend the '%s' class", type.getName(), superClass.getName()));

        while (!Object.class.equals(searchClass))
        {
            if (searchClass.getSuperclass().equals(superClass))
            {
                Type superType = searchClass.getGenericSuperclass();
                if (superType instanceof ParameterizedType)
                {
                    return stream(((ParameterizedType) superType).getActualTypeArguments()).collect(toList());
                }
            }
            searchClass = searchClass.getSuperclass();
        }
        return new LinkedList<>();
    }

    public static void checkInstantiable(Class<?> declaringClass)
    {
        checkInstantiable(declaringClass, true);
    }

    public static void checkInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor)
    {
        if (!isInstantiable(declaringClass, requireDefaultConstructor))
        {
            throw new IllegalArgumentException(String.format("Class %s cannot be instantiated.", declaringClass));
        }
    }

    public static boolean isInstantiable(Class<?> declaringClass)
    {
        return isInstantiable(declaringClass, true);
    }

    public static boolean isInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor)
    {
        return declaringClass != null
               && (!requireDefaultConstructor || hasDefaultConstructor(declaringClass))
               && !declaringClass.isInterface()
               && !Modifier.isAbstract(declaringClass.getModifiers());
    }

    public static boolean isRequired(AccessibleObject object)
    {
        return object.getAnnotation(Optional.class) == null;
    }

    public static boolean isRequired(ParameterModel parameterModel, boolean forceOptional)
    {
        return !forceOptional && parameterModel.isRequired();
    }

    public static boolean isNullType(MetadataType type)
    {
        return type instanceof NullType;
    }

    public static boolean isVoid(Method method)
    {
        return isVoid(method.getReturnType());
    }

    public static boolean isVoid(ComponentModel componentModel)
    {
        return componentModel.getReturnType() instanceof NullType;
    }

    private static boolean isVoid(Class<?> type)
    {
        return type.equals(void.class) || type.equals(Void.class);
    }

    public static Collection<Field> getParameterFields(Class<?> extensionType)
    {
        return getAnnotatedFields(extensionType, Parameter.class);
    }

    public static Collection<Field> getParameterGroupFields(Class<?> extensionType)
    {
        ImmutableList.Builder<Field> listFieldsBuilder = ImmutableList.builder();
        return listFieldsBuilder.addAll(getAnnotatedFields(extensionType, ParameterGroup.class))
                .addAll(getAnnotatedFields(extensionType, MetadataKeyId.class))
                .build();
    }

    public static Collection<Method> getOperationMethods(Class<?> declaringClass)
    {
        return getAllMethods(declaringClass, withModifier(Modifier.PUBLIC), Predicates.not(withAnnotation(Ignore.class)));
    }

    public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType)
    {
        return getDescendingHierarchy(clazz).stream()
                .flatMap(type -> stream(type.getDeclaredFields()))
                .filter(field -> field.getAnnotation(annotationType) != null)
                .collect(new ImmutableListCollector<>());
    }

    private static List<Class<?>> getDescendingHierarchy(Class<?> type)
    {
        List<Class<?>> types = new LinkedList<>();
        types.add(type);
        for (type = type.getSuperclass(); type != null && !Object.class.equals(type); type = type.getSuperclass())
        {
            types.add(0, type);
        }

        return ImmutableList.copyOf(types);
    }

    public static Collection<Field> getExposedFields(Class<?> extensionType)
    {
        Collection<Field> allFields = getAnnotatedFields(extensionType, Parameter.class);
        if (!allFields.isEmpty())
        {
            return allFields;
        }
        try
        {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(extensionType).getPropertyDescriptors();
            return stream(propertyDescriptors)
                    .filter(p -> getField(extensionType, p.getName(), p.getPropertyType()) != null)
                    .map(p -> getField(extensionType, p.getName(), p.getPropertyType()))
                    .collect(toSet());
        }
        catch (IntrospectionException e)
        {
            throw new IllegalModelDefinitionException("Could not introspect POJO: " + extensionType.getName(), e);
        }
    }

    public static ExpressionSupport getExpressionSupport(AnnotatedElement object)
    {
        return getExpressionSupport(object.getAnnotation(Expression.class));
    }

    public static ExpressionSupport getExpressionSupport(Expression expressionAnnotation)
    {
        return expressionAnnotation != null ? expressionAnnotation.value() : SUPPORTED;
    }

    public static String getAliasName(MetadataType metadataType)
    {
        Class<?> type = JavaTypeUtils.getType(metadataType);
        return getAliasName(metadataType, type.getSimpleName());
    }

    public static String getAliasName(MetadataType metadataType, String defaultName)
    {
        Class<?> type = JavaTypeUtils.getType(metadataType);
        return getAliasName(defaultName, type.getAnnotation(Alias.class));
    }

    public static String getAliasName(String defaultName, Alias aliasAnnotation)
    {
        String alias = aliasAnnotation != null ? aliasAnnotation.value() : null;
        return StringUtils.isEmpty(alias) ? defaultName : alias;
    }

    public static String getAlias(Field field)
    {
        Alias alias = field.getAnnotation(Alias.class);
        String name = alias != null ? alias.value() : EMPTY;
        return StringUtils.isEmpty(name) ? field.getName() : name;
    }

    public static String getSourceName(Class<? extends Source> sourceType)
    {
        Alias alias = sourceType.getAnnotation(Alias.class);
        if (alias != null)
        {
            return alias.value();
        }

        return sourceType.getSimpleName();
    }

    public static java.util.Optional<ParameterModel> getContentParameter(ComponentModel component)
    {
        return component.getParameterModels().stream()
                .filter(p -> p.getModelProperty(MetadataContentModelProperty.class).isPresent())
                .findFirst();
    }

    public static List<ParameterModel> getMetadataKeyParts(ComponentModel component)
    {
        return component.getParameterModels().stream()
                .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
                .collect(toList());
    }

    /**
     * Looks for the annotation in the given class. If the annotation is not found, it keeps looking recursively
     * for it in the superClass until it finds it or there is no superClass to analyze.
     */
    public static <T extends Annotation> T getAnnotation(Class<?> annotatedClass, Class<T> annotationClass)
    {
        T annotation = annotatedClass.getAnnotation(annotationClass);
        Class<?> superClass = annotatedClass.getSuperclass();
        while (annotation == null && superClass != null && !superClass.equals(Object.class))
        {
            annotation = superClass.getAnnotation(annotationClass);
            superClass = superClass.getSuperclass();
        }
        return annotation;
    }

    /**
     * Traverses through all the {@link ParameterModel}s of the {@code extensionModel}
     * and returns the {@link Class classes} that are modeled by each parameter's {@link ParameterModel#getType()}.
     * <p>
     * This includes every single {@link ParameterModel} in the model, including configs, providers,
     * operations, etc.
     *
     * @param extensionModel a {@link ExtensionModel}
     * @param filter         a {@link Predicate} used to filter out the classes which are not of interest
     * @return a non {@code null} {@link Set}
     */
    public static Set<Class<?>> getParameterClasses(ExtensionModel extensionModel, Predicate<ParameterModel> filter)
    {
        ImmutableSet.Builder<Class<?>> parameterClasses = ImmutableSet.builder();

        collectParameterClasses(parameterClasses,
                                filter,
                                extensionModel.getConnectionProviders(),
                                extensionModel.getConfigurationModels(),
                                extensionModel.getOperationModels(),
                                extensionModel.getSourceModels());

        extensionModel.getConfigurationModels().forEach(configuration ->
                                                                collectParameterClasses(parameterClasses,
                                                                                        filter,
                                                                                        configuration.getConnectionProviders(),
                                                                                        configuration.getOperationModels(),
                                                                                        configuration.getSourceModels())
        );

        return parameterClasses.build();
    }

    private static void collectParameterClasses(ImmutableSet.Builder<Class<?>> parameterClasses,
                                                Predicate<ParameterModel> filter,
                                                Collection<? extends ParametrizedModel>... parametrizedModelsArray)
    {
        stream(parametrizedModelsArray).forEach(models -> models.stream().forEach(
                model -> model.getParameterModels().stream()
                        .filter(filter)
                        .forEach(parameter -> parameterClasses.add(getType(parameter.getType())))));
    }
}