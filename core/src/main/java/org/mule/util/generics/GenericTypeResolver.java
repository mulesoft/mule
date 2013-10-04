/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.generics;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper class for resolving generic types against type variables.
 * <p/>
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 * <p/>
 * author: Spring
 */
public abstract class GenericTypeResolver
{

    /**
     * Cache from Class to TypeVariable Map
     */
    private static final Map<Class, Reference<Map<TypeVariable, Type>>> typeVariableCache =
            Collections.synchronizedMap(new WeakHashMap<Class, Reference<Map<TypeVariable, Type>>>());


    private GenericTypeResolver()
    {
        // do not isntantiate
    }

    /**
     * Determine the target type for the given parameter specification.
     *
     * @param methodParam the method parameter specification
     * @return the corresponding generic parameter type
     */
    public static Type getTargetType(MethodParameter methodParam)
    {
        if (methodParam.getConstructor() != null)
        {
            return methodParam.getConstructor().getGenericParameterTypes()[methodParam.getParameterIndex()];
        }
        else
        {
            if (methodParam.getParameterIndex() >= 0)
            {
                return methodParam.getMethod().getGenericParameterTypes()[methodParam.getParameterIndex()];
            }
            else
            {
                return methodParam.getMethod().getGenericReturnType();
            }
        }
    }

    /**
     * Determine the target type for the given generic parameter type.
     *
     * @param methodParam the method parameter specification
     * @param clazz       the class to resolve type variables against
     * @return the corresponding generic parameter or return type
     */
    public static Class<?> resolveParameterType(MethodParameter methodParam, Class clazz)
    {
        Type genericType = getTargetType(methodParam);
        Map<TypeVariable, Type> typeVariableMap = getTypeVariableMap(clazz);
        Type rawType = getRawType(genericType, typeVariableMap);
        Class result = (rawType instanceof Class ? (Class) rawType : methodParam.getParameterType());
        methodParam.setParameterType(result);
        methodParam.typeVariableMap = typeVariableMap;
        return result;
    }

    /**
     * Determine the target type for the generic return type of the given method.
     *
     * @param method the method to introspect
     * @param clazz  the class to resolve type variables against
     * @return the corresponding generic parameter or return type
     */
    public static Class<?> resolveReturnType(Method method, Class clazz)
    {
        Type genericType = method.getGenericReturnType();
        Map<TypeVariable, Type> typeVariableMap = getTypeVariableMap(clazz);
        Type rawType = getRawType(genericType, typeVariableMap);
        return (rawType instanceof Class ? (Class) rawType : method.getReturnType());
    }

    /**
     * Resolve the single type argument of the given generic interface against
     * the given target class which is assumed to implement the generic interface
     * and possibly declare a concrete type for its type variable.
     *
     * @param clazz      the target class to check against
     * @param genericIfc the generic interface to resolve the type argument from
     * @return the resolved type of the argument, or <code>null</code> if not resolvable
     */
    public static Class<?> resolveTypeArgument(Class clazz, Class genericIfc)
    {
        Class[] typeArgs = resolveTypeArguments(clazz, genericIfc);
        if (typeArgs == null)
        {
            return null;
        }
        if (typeArgs.length != 1)
        {
            throw new IllegalArgumentException("Expected 1 type argument on generic interface [" +
                    genericIfc.getName() + "] but found " + typeArgs.length);
        }
        return typeArgs[0];
    }

    /**
     * Resolve the type arguments of the given generic interface against the given
     * target class which is assumed to implement the generic interface and possibly
     * declare concrete types for its type variables.
     *
     * @param clazz      the target class to check against
     * @param genericIfc the generic interface to resolve the type argument from
     * @return the resolved type of each argument, with the array size matching the
     *         number of actual type arguments, or <code>null</code> if not resolvable
     */
    public static Class[] resolveTypeArguments(Class clazz, Class genericIfc)
    {
        return doResolveTypeArguments(clazz, clazz, genericIfc);
    }

    private static Class[] doResolveTypeArguments(Class ownerClass, Class classToIntrospect, Class genericIfc)
    {
        while (classToIntrospect != null)
        {
            Type[] ifcs = classToIntrospect.getGenericInterfaces();
            for (Type ifc : ifcs)
            {
                if (ifc instanceof ParameterizedType)
                {
                    ParameterizedType paramIfc = (ParameterizedType) ifc;
                    Type rawType = paramIfc.getRawType();
                    if (genericIfc.equals(rawType))
                    {
                        Type[] typeArgs = paramIfc.getActualTypeArguments();
                        Class[] result = new Class[typeArgs.length];
                        for (int i = 0; i < typeArgs.length; i++)
                        {
                            Type arg = typeArgs[i];
                            if (arg instanceof TypeVariable)
                            {
                                arg = getTypeVariableMap(ownerClass).get(arg);
                            }
                            result[i] = (arg instanceof Class ? (Class) arg : Object.class);
                        }
                        return result;
                    }
                    else if (genericIfc.isAssignableFrom((Class) rawType))
                    {
                        return doResolveTypeArguments(ownerClass, (Class) rawType, genericIfc);
                    }
                }
                else if (genericIfc.isAssignableFrom((Class) ifc))
                {
                    return doResolveTypeArguments(ownerClass, (Class) ifc, genericIfc);
                }
            }
            classToIntrospect = classToIntrospect.getSuperclass();
        }
        return null;
    }


    /**
     * Resolve the specified generic type against the given TypeVariable map.
     *
     * @param genericType     the generic type to resolve
     * @param typeVariableMap the TypeVariable Map to resolved against
     * @return the type if it resolves to a Class, or <code>Object.class</code> otherwise
     */
    static Class resolveType(Type genericType, Map<TypeVariable, Type> typeVariableMap)
    {
        Type rawType = getRawType(genericType, typeVariableMap);
        return (rawType instanceof Class ? (Class) rawType : Object.class);
    }

    /**
     * Determine the raw type for the given generic parameter type.
     *
     * @param genericType     the generic type to resolve
     * @param typeVariableMap the TypeVariable Map to resolved against
     * @return the resolved raw type
     */
    static Type getRawType(Type genericType, Map<TypeVariable, Type> typeVariableMap)
    {
        Type resolvedType = genericType;
        if (genericType instanceof TypeVariable)
        {
            TypeVariable tv = (TypeVariable) genericType;
            resolvedType = typeVariableMap.get(tv);
            if (resolvedType == null)
            {
                resolvedType = extractBoundForTypeVariable(tv);
            }
        }
        if (resolvedType instanceof ParameterizedType)
        {
            return ((ParameterizedType) resolvedType).getRawType();
        }
        else
        {
            return resolvedType;
        }
    }

    /**
     * Build a mapping of {@link TypeVariable#getName TypeVariable names} to concrete
     * {@link Class} for the specified {@link Class}. Searches all super types,
     * enclosing types and interfaces.
     */
    static Map<TypeVariable, Type> getTypeVariableMap(Class clazz)
    {
        Reference<Map<TypeVariable, Type>> ref = typeVariableCache.get(clazz);
        Map<TypeVariable, Type> typeVariableMap = (ref != null ? ref.get() : null);

        if (typeVariableMap == null)
        {
            typeVariableMap = new HashMap<TypeVariable, Type>();

            // interfaces
            extractTypeVariablesFromGenericInterfaces(clazz.getGenericInterfaces(), typeVariableMap);

            // super class
            Type genericType = clazz.getGenericSuperclass();
            Class type = clazz.getSuperclass();
            while (type != null && !Object.class.equals(type))
            {
                if (genericType instanceof ParameterizedType)
                {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    populateTypeMapFromParameterizedType(pt, typeVariableMap);
                }
                extractTypeVariablesFromGenericInterfaces(type.getGenericInterfaces(), typeVariableMap);
                genericType = type.getGenericSuperclass();
                type = type.getSuperclass();
            }

            // enclosing class
            type = clazz;
            while (type.isMemberClass())
            {
                genericType = type.getGenericSuperclass();
                if (genericType instanceof ParameterizedType)
                {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    populateTypeMapFromParameterizedType(pt, typeVariableMap);
                }
                type = type.getEnclosingClass();
            }

            typeVariableCache.put(clazz, new WeakReference<Map<TypeVariable, Type>>(typeVariableMap));
        }

        return typeVariableMap;
    }

    /**
     * Extracts the bound <code>Type</code> for a given {@link TypeVariable}.
     */
    static Type extractBoundForTypeVariable(TypeVariable typeVariable)
    {
        Type[] bounds = typeVariable.getBounds();
        if (bounds.length == 0)
        {
            return Object.class;
        }
        Type bound = bounds[0];
        if (bound instanceof TypeVariable)
        {
            bound = extractBoundForTypeVariable((TypeVariable) bound);
        }
        return bound;
    }

    private static void extractTypeVariablesFromGenericInterfaces(Type[] genericInterfaces, Map<TypeVariable, Type> typeVariableMap)
    {
        for (Type genericInterface : genericInterfaces)
        {
            if (genericInterface instanceof ParameterizedType)
            {
                ParameterizedType pt = (ParameterizedType) genericInterface;
                populateTypeMapFromParameterizedType(pt, typeVariableMap);
                if (pt.getRawType() instanceof Class)
                {
                    extractTypeVariablesFromGenericInterfaces(
                            ((Class) pt.getRawType()).getGenericInterfaces(), typeVariableMap);
                }
            }
            else if (genericInterface instanceof Class)
            {
                extractTypeVariablesFromGenericInterfaces(
                        ((Class) genericInterface).getGenericInterfaces(), typeVariableMap);
            }
        }
    }

    /**
     * Read the {@link TypeVariable TypeVariables} from the supplied {@link ParameterizedType}
     * and add mappings corresponding to the {@link TypeVariable#getName TypeVariable name} ->
     * concrete type to the supplied {@link Map}.
     * <p>Consider this case:
     * <pre class="code>
     * public interface Foo<S, T> {
     * ..
     * }
     * <p/>
     * public class FooImpl implements Foo<String, Integer> {
     * ..
     * }</pre>
     * For '<code>FooImpl</code>' the following mappings would be added to the {@link Map}:
     * {S=java.lang.String, T=java.lang.Integer}.
     */
    private static void populateTypeMapFromParameterizedType(ParameterizedType type, Map<TypeVariable, Type> typeVariableMap)
    {
        if (type.getRawType() instanceof Class)
        {
            Type[] actualTypeArguments = type.getActualTypeArguments();
            TypeVariable[] typeVariables = ((Class) type.getRawType()).getTypeParameters();
            for (int i = 0; i < actualTypeArguments.length; i++)
            {
                Type actualTypeArgument = actualTypeArguments[i];
                TypeVariable variable = typeVariables[i];
                if (actualTypeArgument instanceof Class)
                {
                    typeVariableMap.put(variable, actualTypeArgument);
                }
                else if (actualTypeArgument instanceof GenericArrayType)
                {
                    typeVariableMap.put(variable, actualTypeArgument);
                }
                else if (actualTypeArgument instanceof ParameterizedType)
                {
                    typeVariableMap.put(variable, actualTypeArgument);
                }
                else if (actualTypeArgument instanceof TypeVariable)
                {
                    // We have a type that is parameterized at instantiation time
                    // the nearest match on the bridge method will be the bounded type.
                    TypeVariable typeVariableArgument = (TypeVariable) actualTypeArgument;
                    Type resolvedType = typeVariableMap.get(typeVariableArgument);
                    if (resolvedType == null) {
                        resolvedType = extractBoundForTypeVariable(typeVariableArgument);
                    }
                    typeVariableMap.put(variable, resolvedType);
                }
            }
        }
    }

}
