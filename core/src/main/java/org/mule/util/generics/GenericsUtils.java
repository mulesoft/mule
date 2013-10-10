/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.generics;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

/**
 * Helper class for determining element types of collections and maps.
 * <p/>
 * <p>Mainly intended for usage within the framework, determining the
 * target type of values to be added to a collection or map
 * (to be able to attempt type conversion if appropriate).
 * <p/>
 * author: Spring
 */
public class GenericsUtils
{


    /**
     * Determine the generic element type of the given Collection class
     * (if it declares one through a generic superclass or generic interface).
     *
     * @param collectionClass the collection class to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionType(Class<? extends Collection<?>> collectionClass)
    {
        return extractTypeFromClass(collectionClass, Collection.class, 0);
    }

    /**
     * Determine the generic key type of the given Map class
     * (if it declares one through a generic superclass or generic interface).
     *
     * @param mapClass the map class to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyType(Class<? extends Map<?, ?>> mapClass)
    {
        return extractTypeFromClass(mapClass, Map.class, 0);
    }

    /**
     * Determine the generic value type of the given Map class
     * (if it declares one through a generic superclass or generic interface).
     *
     * @param mapClass the map class to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueType(Class<? extends Map<?, ?>> mapClass)
    {
        return extractTypeFromClass(mapClass, Map.class, 1);
    }

    /**
     * Determine the generic element type of the given Collection field.
     *
     * @param collectionField the collection field to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionFieldType(Field collectionField)
    {
        return getGenericFieldType(collectionField, Collection.class, 0, 1);
    }

    /**
     * Determine the generic element type of the given Collection field.
     *
     * @param collectionField the collection field to introspect
     * @param nestingLevel    the nesting level of the target type
     *                        (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                        nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionFieldType(Field collectionField, int nestingLevel)
    {
        return getGenericFieldType(collectionField, Collection.class, 0, nestingLevel);
    }

    /**
     * Determine the generic key type of the given Map field.
     *
     * @param mapField the map field to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyFieldType(Field mapField)
    {
        return getGenericFieldType(mapField, Map.class, 0, 1);
    }

    /**
     * Determine the generic key type of the given Map field.
     *
     * @param mapField     the map field to introspect
     * @param nestingLevel the nesting level of the target type
     *                     (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                     nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyFieldType(Field mapField, int nestingLevel)
    {
        return getGenericFieldType(mapField, Map.class, 0, nestingLevel);
    }

    /**
     * Determine the generic value type of the given Map field.
     *
     * @param mapField the map field to introspect
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueFieldType(Field mapField)
    {
        return getGenericFieldType(mapField, Map.class, 1, 1);
    }

    /**
     * Determine the generic value type of the given Map field.
     *
     * @param mapField     the map field to introspect
     * @param nestingLevel the nesting level of the target type
     *                     (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                     nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueFieldType(Field mapField, int nestingLevel)
    {
        return getGenericFieldType(mapField, Map.class, 1, nestingLevel);
    }

    /**
     * Determine the generic element type of the given Collection parameter.
     *
     * @param methodParam the method parameter specification
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionParameterType(MethodParameter methodParam)
    {
        return getGenericParameterType(methodParam, Collection.class, 0);
    }

    /**
     * Determine the generic key type of the given Map parameter.
     *
     * @param methodParam the method parameter specification
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyParameterType(MethodParameter methodParam)
    {
        return getGenericParameterType(methodParam, Map.class, 0);
    }

    /**
     * Determine the generic value type of the given Map parameter.
     *
     * @param methodParam the method parameter specification
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueParameterType(MethodParameter methodParam)
    {
        return getGenericParameterType(methodParam, Map.class, 1);
    }

    /**
     * Determine the generic element type of the given Collection return type.
     *
     * @param method the method to check the return type for
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionReturnType(Method method)
    {
        return getGenericReturnType(method, Collection.class, 0, 1);
    }

    /**
     * Determine the generic element type of the given Collection return type.
     * <p>If the specified nesting level is higher than 1, the element type of
     * a nested Collection/Map will be analyzed.
     *
     * @param method       the method to check the return type for
     * @param nestingLevel the nesting level of the target type
     *                     (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                     nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getCollectionReturnType(Method method, int nestingLevel)
    {
        return getGenericReturnType(method, Collection.class, 0, nestingLevel);
    }

    /**
     * Determine the generic key type of the given Map return type.
     *
     * @param method the method to check the return type for
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyReturnType(Method method)
    {
        return getGenericReturnType(method, Map.class, 0, 1);
    }

    /**
     * Determine the generic key type of the given Map return type.
     *
     * @param method       the method to check the return type for
     * @param nestingLevel the nesting level of the target type
     *                     (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                     nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapKeyReturnType(Method method, int nestingLevel)
    {
        return getGenericReturnType(method, Map.class, 0, nestingLevel);
    }

    /**
     * Determine the generic value type of the given Map return type.
     *
     * @param method the method to check the return type for
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueReturnType(Method method)
    {
        return getGenericReturnType(method, Map.class, 1, 1);
    }

    /**
     * Determine the generic value type of the given Map return type.
     *
     * @param method       the method to check the return type for
     * @param nestingLevel the nesting level of the target type
     *                     (typically 1; e.g. in case of a List of Lists, 1 would indicate the
     *                     nested List, whereas 2 would indicate the element of the nested List)
     * @return the generic type, or <code>null</code> if none
     */
    public static Class<?> getMapValueReturnType(Method method, int nestingLevel)
    {
        return getGenericReturnType(method, Map.class, 1, nestingLevel);
    }


    /**
     * Extract the generic parameter type from the given method or constructor.
     *
     * @param methodParam the method parameter specification
     * @param source      the source class/interface defining the generic parameter types
     * @param typeIndex   the index of the type (e.g. 0 for Collections,
     *                    0 for Map keys, 1 for Map values)
     * @return the generic type, or <code>null</code> if none
     */
    private static Class<?> getGenericParameterType(MethodParameter methodParam, Class<?> source, int typeIndex)
    {
        return extractType(methodParam, GenericTypeResolver.getTargetType(methodParam),
                source, typeIndex, methodParam.getNestingLevel(), 1);
    }

    /**
     * Extract the generic type from the given field.
     *
     * @param field        the field to check the type for
     * @param source       the source class/interface defining the generic parameter types
     * @param typeIndex    the index of the type (e.g. 0 for Collections,
     *                     0 for Map keys, 1 for Map values)
     * @param nestingLevel the nesting level of the target type
     * @return the generic type, or <code>null</code> if none
     */
    private static Class<?> getGenericFieldType(Field field, Class<?> source, int typeIndex, int nestingLevel)
    {
        return extractType(null, field.getGenericType(), source, typeIndex, nestingLevel, 1);
    }

    /**
     * Extract the generic return type from the given method.
     *
     * @param method       the method to check the return type for
     * @param source       the source class/interface defining the generic parameter types
     * @param typeIndex    the index of the type (e.g. 0 for Collections,
     *                     0 for Map keys, 1 for Map values)
     * @param nestingLevel the nesting level of the target type
     * @return the generic type, or <code>null</code> if none
     */
    private static Class<?> getGenericReturnType(Method method, Class<?> source, int typeIndex, int nestingLevel)
    {
        return extractType(null, method.getGenericReturnType(), source, typeIndex, nestingLevel, 1);
    }

    /**
     * Extract the generic type from the given Type object.
     *
     * @param methodParam  the method parameter specification
     * @param type         the Type to check
     * @param source       the source collection/map Class<?> that we check
     * @param typeIndex    the index of the actual type argument
     * @param nestingLevel the nesting level of the target type
     * @param currentLevel the current nested level
     * @return the generic type as Class, or <code>null</code> if none
     */
    private static Class<?> extractType(MethodParameter methodParam, Type type, Class<?> source, 
        int typeIndex, int nestingLevel, int currentLevel)
    {
        Type resolvedType = type;
        if (type instanceof TypeVariable<?> && methodParam != null && methodParam.typeVariableMap != null)
        {
            Type mappedType = methodParam.typeVariableMap.get(type);
            if (mappedType != null)
            {
                resolvedType = mappedType;
            }
        }
        if (resolvedType instanceof ParameterizedType)
        {
            return extractTypeFromParameterizedType(
                    methodParam, (ParameterizedType) resolvedType, source, typeIndex, nestingLevel, currentLevel);
        }
        else if (resolvedType instanceof Class<?>)
        {
            Class<?> resolvedClass = (Class<?>) resolvedType;
            return extractTypeFromClass(methodParam, resolvedClass, source, typeIndex, nestingLevel, currentLevel);
        }
        else
        {
            return null;
        }
    }

    /**
     * Extract the generic type from the given ParameterizedType object.
     *
     * @param methodParam  the method parameter specification
     * @param ptype        the ParameterizedType to check
     * @param source       the expected raw source type (can be <code>null</code>)
     * @param typeIndex    the index of the actual type argument
     * @param nestingLevel the nesting level of the target type
     * @param currentLevel the current nested level
     * @return the generic type as Class, or <code>null</code> if none
     */
    private static Class<?> extractTypeFromParameterizedType(MethodParameter methodParam,
         ParameterizedType ptype, Class<?> source, int typeIndex, int nestingLevel, int currentLevel)
    {

        if (!(ptype.getRawType() instanceof Class<?>))
        {
            return null;
        }
        
        Class<?> rawType = (Class<?>) ptype.getRawType();
        Type[] paramTypes = ptype.getActualTypeArguments();
        if (nestingLevel - currentLevel > 0)
        {
            int nextLevel = currentLevel + 1;
            Integer currentTypeIndex = (methodParam != null ? methodParam.getTypeIndexForLevel(nextLevel) : null);
            // Default is last parameter type: Collection element or Map value.
            int indexToUse = (currentTypeIndex != null ? currentTypeIndex : paramTypes.length - 1);
            Type paramType = paramTypes[indexToUse];
            return extractType(methodParam, paramType, source, typeIndex, nestingLevel, nextLevel);
        }
        if (source != null && !source.isAssignableFrom(rawType))
        {
            return null;
        }
        Class<?> fromSuperclassOrInterface =
                extractTypeFromClass(methodParam, rawType, source, typeIndex, nestingLevel, currentLevel);
        if (fromSuperclassOrInterface != null)
        {
            return fromSuperclassOrInterface;
        }
        if (paramTypes == null || typeIndex >= paramTypes.length)
        {
            return null;
        }
        Type paramType = paramTypes[typeIndex];
        if (paramType instanceof TypeVariable<?> && methodParam != null && methodParam.typeVariableMap != null)
        {
            Type mappedType = methodParam.typeVariableMap.get(paramType);
            if (mappedType != null)
            {
                paramType = mappedType;
            }
        }
        if (paramType instanceof WildcardType)
        {
            WildcardType wildcardType = (WildcardType) paramType;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds != null && upperBounds.length > 0 && !Object.class.equals(upperBounds[0]))
            {
                paramType = upperBounds[0];
            }
            else
            {
                Type[] lowerBounds = wildcardType.getLowerBounds();
                if (lowerBounds != null && lowerBounds.length > 0 && !Object.class.equals(lowerBounds[0]))
                {
                    paramType = lowerBounds[0];
                }
            }
        }
        if (paramType instanceof ParameterizedType)
        {
            paramType = ((ParameterizedType) paramType).getRawType();
        }
        if (paramType instanceof GenericArrayType)
        {
            // A generic array type... Let's turn it into a straight array type if possible.
            Type compType = ((GenericArrayType) paramType).getGenericComponentType();
            if (compType instanceof Class<?>)
            {
                Class<?> compClass = (Class<?>) compType;
                return Array.newInstance(compClass, 0).getClass();
            }
        }
        else if (paramType instanceof Class<?>)
        {
            // We finally got a straight Class...
            return (Class<?>) paramType;
        }
        return null;
    }

    /**
     * Extract the generic type from the given Class<?> object.
     *
     * @param clazz     the Class<?> to check
     * @param source    the expected raw source type (can be <code>null</code>)
     * @param typeIndex the index of the actual type argument
     * @return the generic type as Class, or <code>null</code> if none
     */
    private static Class<?> extractTypeFromClass(Class<?> clazz, Class<?> source, int typeIndex)
    {
        return extractTypeFromClass(null, clazz, source, typeIndex, 1, 1);
    }

    /**
     * Extract the generic type from the given Class<?> object.
     *
     * @param methodParam  the method parameter specification
     * @param clazz        the Class<?> to check
     * @param source       the expected raw source type (can be <code>null</code>)
     * @param typeIndex    the index of the actual type argument
     * @param nestingLevel the nesting level of the target type
     * @param currentLevel the current nested level
     * @return the generic type as Class, or <code>null</code> if none
     */
    private static Class<?> extractTypeFromClass(
            MethodParameter methodParam, Class<?> clazz, Class<?> source, int typeIndex, int nestingLevel, int currentLevel)
    {

        if (clazz.getName().startsWith("java.util."))
        {
            return null;
        }
        if (clazz.getSuperclass() != null && isIntrospectionCandidate(clazz.getSuperclass()))
        {
            return extractType(methodParam, clazz.getGenericSuperclass(), source, typeIndex, nestingLevel, currentLevel);
        }
        Type[] ifcs = clazz.getGenericInterfaces();
        if (ifcs != null)
        {
            for (Type ifc : ifcs)
            {
                Type rawType = ifc;
                if (ifc instanceof ParameterizedType)
                {
                    rawType = ((ParameterizedType) ifc).getRawType();
                }
                if (rawType instanceof Class<?> && isIntrospectionCandidate((Class<?>) rawType))
                {
                    return extractType(methodParam, ifc, source, typeIndex, nestingLevel, currentLevel);
                }
            }
        }
        return null;
    }

    /**
     * Determine whether the given class is a potential candidate
     * that defines generic collection or map types.
     *
     * @param clazz the class to check
     * @return whether the given class is assignable to Collection or Map
     */
    private static boolean isIntrospectionCandidate(Class<?> clazz)
    {
        return (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz));
    }

}

