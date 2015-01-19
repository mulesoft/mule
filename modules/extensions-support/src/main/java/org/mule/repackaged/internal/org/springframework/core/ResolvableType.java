/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.repackaged.internal.org.springframework.core;

import static org.mule.repackaged.internal.org.springframework.core.SerializableTypeWrapper.FieldTypeProvider;
import static org.mule.repackaged.internal.org.springframework.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import static org.mule.repackaged.internal.org.springframework.core.SerializableTypeWrapper.TypeProvider;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.repackaged.internal.org.springframework.util.ConcurrentReferenceHashMap;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;

import com.google.common.base.Joiner;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 * <p/>
 * <p>{@code ResolvableTypes} may be obtained from {@link #forField(Field) fields},
 * {@link #forMethodParameter(Method, int) method parameters},
 * {@link #forMethodReturnType(Method) method returns} or
 * {@link #forClass(Class) classes}. Most methods on this class will themselves return
 * {@link ResolvableType}s, allowing easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 * <p/>
 * public void example() {
 * ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 * t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 * t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 * t.getGeneric(0).resolve(); // Integer
 * t.getGeneric(1).resolve(); // List
 * t.getGeneric(1); // List&lt;String&gt;
 * t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturnType(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @since 4.0
 */
@SuppressWarnings("serial")
public final class ResolvableType implements Serializable
{

    /**
     * {@code ResolvableType} returned when no value is available. {@code NONE} is used
     * in preference to {@code null} so that multiple method calls can be safely chained.
     */
    public static final ResolvableType NONE = new ResolvableType(null, null, null, null);

    private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

    private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
            new ConcurrentReferenceHashMap<>(256);


    /**
     * The underlying Java type being managed (only ever {@code null} for {@link #NONE}).
     */
    private final Type type;

    /**
     * Optional provider for the type.
     */
    private final TypeProvider typeProvider;

    /**
     * The {@code VariableResolver} to use or {@code null} if no resolver is available.
     */
    private final VariableResolver variableResolver;

    /**
     * The component type for an array or {@code null} if the type should be deduced.
     */
    private final ResolvableType componentType;

    /**
     * Copy of the resolved value.
     */
    private final Class<?> resolved;

    private ResolvableType superType;

    private ResolvableType[] interfaces;

    private ResolvableType[] generics;


    /**
     * Private constructor used to create a new {@link ResolvableType} for resolution purposes.
     */
    private ResolvableType(
            Type type, TypeProvider typeProvider, VariableResolver variableResolver, ResolvableType componentType)
    {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.resolved = resolveClass();
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} for cache key purposes.
     */
    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver)
    {
        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.resolved = null;
    }


    /**
     * Return the underling Java {@link Type} being managed. With the exception of
     * the {@link #NONE} constant, this method will never return {@code null}.
     */
    public Type getType()
    {
        return SerializableTypeWrapper.unwrap(this.type);
    }

    /**
     * Return the underlying Java {@link Class} being  managed, if available;
     * otherwise {@code null}.
     */
    public Class<?> getRawClass()
    {
        Type rawType = this.type;
        if (rawType instanceof ParameterizedType)
        {
            rawType = ((ParameterizedType) rawType).getRawType();
        }
        return (rawType instanceof Class ? (Class<?>) rawType : null);
    }

    /**
     * Return the underlying source of the resolvable type. Will return a {@link Field},
     * {@link MethodParameter} or {@link Type} depending on how the {@link ResolvableType}
     * was constructed. With the exception of the {@link #NONE} constant, this method will
     * never return {@code null}. This method is primarily to provide access to additional
     * type information or meta-data that alternative JVM languages may provide.
     */
    public Object getSource()
    {
        Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
        return (source != null ? source : this.type);
    }

    /**
     * Determine whether this {@code ResolvableType} is assignable from the
     * specified other type.
     * <p>Attempts to follow the same rules as the Java compiler, considering
     * whether both the {@link #resolve() resolved} {@code Class} is
     * {@link Class#isAssignableFrom(Class) assignable from} the given type
     * as well as whether all {@link #getGenerics() generics} are assignable.
     *
     * @param other the type to be checked against
     * @return {@code true} if the specified other type can be assigned to this
     * {@code ResolvableType}; {@code false} otherwise
     */
    public boolean isAssignableFrom(ResolvableType other)
    {
        return isAssignableFrom(other, null);
    }

    private boolean isAssignableFrom(ResolvableType other, Map<Type, Type> matchedBefore)
    {
        checkArgument(other != null, "ResolvableType must not be null");

        // If we cannot resolve types, we are not assignable
        if (this == NONE || other == NONE)
        {
            return false;
        }

        // Deal with array by delegating to the component type
        if (isArray())
        {
            return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
        }

        if (matchedBefore != null && matchedBefore.get(this.type) == other.type)
        {
            return true;
        }

        // Deal with wildcard bounds
        WildcardBounds ourBounds = WildcardBounds.get(this);
        WildcardBounds typeBounds = WildcardBounds.get(other);

        // In the from X is assignable to <? extends Number>
        if (typeBounds != null)
        {
            return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
                    ourBounds.isAssignableFrom(typeBounds.getBounds()));
        }

        // In the form <? extends Number> is assignable to X...
        if (ourBounds != null)
        {
            return ourBounds.isAssignableFrom(other);
        }

        // Main assignability check about to follow
        boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now...
        boolean checkGenerics = true;
        Class<?> ourResolved = null;
        if (this.type instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null)
            {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null)
                {
                    ourResolved = resolved.resolve();
                }
            }
            if (ourResolved == null)
            {
                // Try variable resolution against target type
                if (other.variableResolver != null)
                {
                    ResolvableType resolved = other.variableResolver.resolveVariable(variable);
                    if (resolved != null)
                    {
                        ourResolved = resolved.resolve();
                        checkGenerics = false;
                    }
                }
            }
            if (ourResolved == null)
            {
                // Unresolved type variable, potentially nested -> never insist on exact match
                exactMatch = false;
            }
        }
        if (ourResolved == null)
        {
            ourResolved = resolve(Object.class);
        }
        Class<?> otherResolved = other.resolve(Object.class);

        // We need an exact type match for generics
        // List<CharSequence> is not assignable from List<String>
        if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved))
        {
            return false;
        }

        if (checkGenerics)
        {
            // Recursively check each generic
            ResolvableType[] ourGenerics = getGenerics();
            ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
            if (ourGenerics.length != typeGenerics.length)
            {
                return false;
            }
            if (matchedBefore == null)
            {
                matchedBefore = new IdentityHashMap<Type, Type>(1);
            }
            matchedBefore.put(this.type, other.type);
            for (int i = 0; i < ourGenerics.length; i++)
            {
                if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Return {@code true} if this type resolves to a Class that represents an array.
     *
     * @see #getComponentType()
     */
    public boolean isArray()
    {
        if (this == NONE)
        {
            return false;
        }
        return (((this.type instanceof Class && ((Class<?>) this.type).isArray())) ||
                this.type instanceof GenericArrayType || resolveType().isArray());
    }

    /**
     * Return the ResolvableType representing the component type of the array or
     * {@link #NONE} if this type does not represent an array.
     *
     * @see #isArray()
     */
    public ResolvableType getComponentType()
    {
        if (this == NONE)
        {
            return NONE;
        }
        if (this.componentType != null)
        {
            return this.componentType;
        }
        if (this.type instanceof Class)
        {
            Class<?> componentType = ((Class<?>) this.type).getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType)
        {
            return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }

    /**
     * Convenience method to return this type as a resolvable {@link Collection} type.
     * Returns {@link #NONE} if this type does not implement or extend
     * {@link Collection}.
     *
     * @see #as(Class)
     * @see #asMap()
     */
    public ResolvableType asCollection()
    {
        return as(Collection.class);
    }

    /**
     * Convenience method to return this type as a resolvable {@link Map} type.
     * Returns {@link #NONE} if this type does not implement or extend
     * {@link Map}.
     *
     * @see #as(Class)
     * @see #asCollection()
     */
    public ResolvableType asMap()
    {
        return as(Map.class);
    }

    /**
     * Return this type as a {@link ResolvableType} of the specified class. Searches
     * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
     * hierarchies to find a match, returning {@link #NONE} if this type does not
     * implement or extend the specified class.
     *
     * @param type the required class type
     * @return a {@link ResolvableType} representing this object as the specified
     * type or {@link #NONE}
     * @see #asCollection()
     * @see #asMap()
     * @see #getSuperType()
     * @see #getInterfaces()
     */
    public ResolvableType as(Class<?> type)
    {
        if (this == NONE)
        {
            return NONE;
        }
        if (Objects.equals(resolve(), type))
        {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces())
        {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (interfaceAsType != NONE)
            {
                return interfaceAsType;
            }
        }
        return getSuperType().as(type);
    }

    /**
     * Return a {@link ResolvableType} representing the direct supertype of this type.
     * If no supertype is available this method returns {@link #NONE}.
     *
     * @see #getInterfaces()
     */
    public ResolvableType getSuperType()
    {
        Class<?> resolved = resolve();
        if (resolved == null || resolved.getGenericSuperclass() == null)
        {
            return NONE;
        }
        if (this.superType == null)
        {
            this.superType = forType(SerializableTypeWrapper.forGenericSuperclass(resolved),
                                     asVariableResolver());
        }
        return this.superType;
    }

    /**
     * Return a {@link ResolvableType} array representing the direct interfaces
     * implemented by this type. If this type does not implement any interfaces an
     * empty array is returned.
     *
     * @see #getSuperType()
     */
    public ResolvableType[] getInterfaces()
    {
        Class<?> resolved = resolve();
        if (resolved == null || ArrayUtils.isEmpty(resolved.getGenericInterfaces()))
        {
            return EMPTY_TYPES_ARRAY;
        }
        if (this.interfaces == null)
        {
            this.interfaces = forTypes(SerializableTypeWrapper.forGenericInterfaces(resolved),
                                       asVariableResolver());
        }
        return this.interfaces;
    }

    /**
     * Return {@code true} if this type contains generic parameters.
     *
     * @see #getGeneric(int...)
     * @see #getGenerics()
     */
    public boolean hasGenerics()
    {
        return (getGenerics().length > 0);
    }

    /**
     * Return {@code true} if this type contains unresolvable generics only,
     * that is, no substitute for any of its declared type variables.
     */
    boolean isEntirelyUnresolvable()
    {
        if (this == NONE)
        {
            return false;
        }
        ResolvableType[] generics = getGenerics();
        for (ResolvableType generic : generics)
        {
            if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine whether the underlying type has any unresolvable generics:
     * either through an unresolvable type variable on the type itself
     * or through implementing a generic interface in a raw fashion,
     * i.e. without substituting that interface's type variables.
     * The result will be {@code true} only in those two scenarios.
     */
    public boolean hasUnresolvableGenerics()
    {
        if (this == NONE)
        {
            return false;
        }
        ResolvableType[] generics = getGenerics();
        for (ResolvableType generic : generics)
        {
            if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds())
            {
                return true;
            }
        }
        Class<?> resolved = resolve();
        if (resolved != null)
        {
            for (Type genericInterface : resolved.getGenericInterfaces())
            {
                if (genericInterface instanceof Class)
                {
                    if (forClass((Class<?>) genericInterface).hasGenerics())
                    {
                        return true;
                    }
                }
            }
            return getSuperType().hasUnresolvableGenerics();
        }
        return false;
    }

    /**
     * Determine whether the underlying type is a type variable that
     * cannot be resolved through the associated variable resolver.
     */
    private boolean isUnresolvableTypeVariable()
    {
        if (this.type instanceof TypeVariable)
        {
            if (this.variableResolver == null)
            {
                return true;
            }
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            ResolvableType resolved = this.variableResolver.resolveVariable(variable);
            if (resolved == null || resolved.isUnresolvableTypeVariable())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether the underlying type represents a wildcard
     * without specific bounds (i.e., equal to {@code ? extends Object}).
     */
    private boolean isWildcardWithoutBounds()
    {
        if (this.type instanceof WildcardType)
        {
            WildcardType wt = (WildcardType) this.type;
            if (wt.getLowerBounds().length == 0)
            {
                Type[] upperBounds = wt.getUpperBounds();
                if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class.equals(upperBounds[0])))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return a {@link ResolvableType} for the specified nesting level. See
     * {@link #getNested(int, Map)} for details.
     *
     * @param nestingLevel the nesting level
     * @return the {@link ResolvableType} type, or {@code #NONE}
     */
    public ResolvableType getNested(int nestingLevel)
    {
        return getNested(nestingLevel, null);
    }

    /**
     * Return a {@link ResolvableType} for the specified nesting level. The nesting level
     * refers to the specific generic parameter that should be returned. A nesting level
     * of 1 indicates this type; 2 indicates the first nested generic; 3 the second; and so
     * on. For example, given {@code List<Set<Integer>>} level 1 refers to the
     * {@code List}, level 2 the {@code Set}, and level 3 the {@code Integer}.
     * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
     * for the given level. For example, an index of 0 would refer to a {@code Map} key;
     * whereas, 1 would refer to the value. If the map does not contain a value for a
     * specific level the last generic will be used (e.g. a {@code Map} value).
     * <p>Nesting levels may also apply to array types; for example given
     * {@code String[]}, a nesting level of 2 refers to {@code String}.
     * <p>If a type does not {@link #hasGenerics() contain} generics the
     * {@link #getSuperType() supertype} hierarchy will be considered.
     *
     * @param nestingLevel        the required nesting level, indexed from 1 for the current
     *                            type, 2 for the first nested generic, 3 for the second and so on
     * @param typeIndexesPerLevel a map containing the generic index for a given nesting
     *                            level (may be {@code null})
     * @return a {@link ResolvableType} for the nested level or {@link #NONE}
     */
    public ResolvableType getNested(int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel)
    {
        ResolvableType result = this;
        for (int i = 2; i <= nestingLevel; i++)
        {
            if (result.isArray())
            {
                result = result.getComponentType();
            }
            else
            {
                // Handle derived types
                while (result != ResolvableType.NONE && !result.hasGenerics())
                {
                    result = result.getSuperType();
                }
                Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
                index = (index == null ? result.getGenerics().length - 1 : index);
                result = result.getGeneric(index);
            }
        }
        return result;
    }

    /**
     * Return a {@link ResolvableType} representing the generic parameter for the given
     * indexes. Indexes are zero based; for example given the type
     * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
     * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
     * for example {@code getGeneric(1, 0)} will access the {@code String} from the nested
     * {@code List}. For convenience, if no indexes are specified the first generic is
     * returned.
     * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
     *
     * @param indexes the indexes that refer to the generic parameter (may be omitted to
     *                return the first generic)
     * @return a {@link ResolvableType} for the specified generic or {@link #NONE}
     * @see #hasGenerics()
     * @see #getGenerics()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType getGeneric(int... indexes)
    {
        try
        {
            if (indexes == null || indexes.length == 0)
            {
                return getGenerics()[0];
            }
            ResolvableType generic = this;
            for (int index : indexes)
            {
                generic = generic.getGenerics()[index];
            }
            return generic;
        }
        catch (IndexOutOfBoundsException ex)
        {
            return NONE;
        }
    }

    /**
     * Return an array of {@link ResolvableType}s representing the generic parameters of
     * this type. If no generics are available an empty array is returned. If you need to
     * access a specific generic consider using the {@link #getGeneric(int...)} method as
     * it allows access to nested generics and protects against
     * {@code IndexOutOfBoundsExceptions}.
     *
     * @return an array of {@link ResolvableType}s representing the generic parameters
     * (never {@code null})
     * @see #hasGenerics()
     * @see #getGeneric(int...)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType[] getGenerics()
    {
        if (this == NONE)
        {
            return EMPTY_TYPES_ARRAY;
        }
        if (this.generics == null)
        {
            if (this.type instanceof Class<?>)
            {
                Class<?> typeClass = (Class<?>) this.type;
                this.generics = forTypes(SerializableTypeWrapper.forTypeParameters(typeClass), this.variableResolver);
            }
            else if (this.type instanceof ParameterizedType)
            {
                Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
                ResolvableType[] generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++)
                {
                    generics[i] = forType(actualTypeArguments[i], this.variableResolver);
                }
                this.generics = generics;
            }
            else
            {
                this.generics = resolveType().getGenerics();
            }
        }
        return this.generics;
    }

    /**
     * Convenience method that will {@link #getGenerics() get} and
     * {@link #resolve() resolve} generic parameters.
     *
     * @return an array of resolved generic parameters (the resulting array
     * will never be {@code null}, but it may contain {@code null} elements})
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics()
    {
        return resolveGenerics(null);
    }

    /**
     * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
     * resolve} generic parameters, using the specified {@code fallback} if any type
     * cannot be resolved.
     *
     * @param fallback the fallback class to use if resolution fails (may be {@code null})
     * @return an array of resolved generic parameters (the resulting array will never be
     * {@code null}, but it may contain {@code null} elements})
     * @see #getGenerics()
     * @see #resolve()
     */
    public Class<?>[] resolveGenerics(Class<?> fallback)
    {
        ResolvableType[] generics = getGenerics();
        Class<?>[] resolvedGenerics = new Class<?>[generics.length];
        for (int i = 0; i < generics.length; i++)
        {
            resolvedGenerics[i] = generics[i].resolve(fallback);
        }
        return resolvedGenerics;
    }

    /**
     * Convenience method that will {@link #getGeneric(int...) get} and
     * {@link #resolve() resolve} a specific generic parameters.
     *
     * @param indexes the indexes that refer to the generic parameter
     *                (may be omitted to return the first generic)
     * @return a resolved {@link Class} or {@code null}
     * @see #getGeneric(int...)
     * @see #resolve()
     */
    public Class<?> resolveGeneric(int... indexes)
    {
        return getGeneric(indexes).resolve();
    }

    /**
     * Resolve this type to a {@link java.lang.Class}, returning {@code null}
     * if the type cannot be resolved. This method will consider bounds of
     * {@link TypeVariable}s and {@link WildcardType}s if direct resolution fails;
     * however, bounds of {@code Object.class} will be ignored.
     *
     * @return the resolved {@link Class}, or {@code null} if not resolvable
     * @see #resolve(Class)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve()
    {
        return resolve(null);
    }

    /**
     * Resolve this type to a {@link java.lang.Class}, returning the specified
     * {@code fallback} if the type cannot be resolved. This method will consider bounds
     * of {@link TypeVariable}s and {@link WildcardType}s if direct resolution fails;
     * however, bounds of {@code Object.class} will be ignored.
     *
     * @param fallback the fallback class to use if resolution fails (may be {@code null})
     * @return the resolved {@link Class} or the {@code fallback}
     * @see #resolve()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve(Class<?> fallback)
    {
        return (this.resolved != null ? this.resolved : fallback);
    }

    private Class<?> resolveClass()
    {
        if (this.type instanceof Class<?> || this.type == null)
        {
            return (Class<?>) this.type;
        }
        if (this.type instanceof GenericArrayType)
        {
            Class<?> resolvedComponent = getComponentType().resolve();
            return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
        }
        return resolveType().resolve();
    }

    /**
     * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
     * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
     * as it cannot be serialized.
     */
    ResolvableType resolveType()
    {
        if (this.type instanceof ParameterizedType)
        {
            return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType)
        {
            Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
            if (resolved == null)
            {
                resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
            }
            return forType(resolved, this.variableResolver);
        }
        if (this.type instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // Try default variable resolution
            if (this.variableResolver != null)
            {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null)
                {
                    return resolved;
                }
            }
            // Fallback to bounds
            return forType(resolveBounds(variable.getBounds()), this.variableResolver);
        }
        return NONE;
    }

    private Type resolveBounds(Type[] bounds)
    {
        if (bounds == null || bounds.length == 0 || Object.class.equals(bounds[0]))
        {
            return null;
        }
        return bounds[0];
    }

    private ResolvableType resolveVariable(TypeVariable<?> variable)
    {
        if (this.type instanceof TypeVariable)
        {
            return resolveType().resolveVariable(variable);
        }
        if (this.type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) this.type;
            TypeVariable<?>[] variables = resolve().getTypeParameters();
            for (int i = 0; i < variables.length; i++)
            {
                if (Objects.equals(variables[i].getName(), variable.getName()))
                {
                    Type actualType = parameterizedType.getActualTypeArguments()[i];
                    return forType(actualType, this.variableResolver);
                }
            }
            if (parameterizedType.getOwnerType() != null)
            {
                return forType(parameterizedType.getOwnerType(), this.variableResolver).resolveVariable(variable);
            }
        }
        if (this.variableResolver != null)
        {
            return this.variableResolver.resolveVariable(variable);
        }
        return null;
    }

    /**
     * Return a String representation of this type in its fully resolved form
     * (including any generic parameters).
     */
    @Override
    public String toString()
    {
        if (isArray())
        {
            return getComponentType() + "[]";
        }
        if (this.resolved == null)
        {
            return "?";
        }
        if (this.type instanceof TypeVariable)
        {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null)
            {
                // Don't bother with variable boundaries for toString()...
                // Can cause infinite recursions in case of self-references
                return "?";
            }
        }
        StringBuilder result = new StringBuilder(this.resolved.getName());
        if (hasGenerics())
        {
            result.append('<')
                    .append(Joiner.on(", ").join(getGenerics()))
                    .append('>');
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof ResolvableType))
        {
            return false;
        }
        ResolvableType other = (ResolvableType) obj;
        return (Objects.equals(this.type, other.type) &&
                Objects.equals(getSource(), other.getSource()) &&
                variableResolverSourceEquals(other.variableResolver) &&
                Objects.equals(this.componentType, other.componentType));
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(type);
    }

    /**
     * Custom serialization support for {@link #NONE}.
     */
    private Object readResolve() throws ObjectStreamException
    {
        return (this.type == null ? NONE : this);
    }

    /**
     * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
     */
    VariableResolver asVariableResolver()
    {
        if (this == NONE)
        {
            return null;
        }
        return new DefaultVariableResolver();
    }

    private boolean variableResolverSourceEquals(VariableResolver other)
    {
        if (this.variableResolver == null)
        {
            return (other == null);
        }
        if (other == null)
        {
            return false;
        }
        return Objects.equals(this.variableResolver.getSource(), other.getSource());
    }

    private static ResolvableType[] forTypes(Type[] types, VariableResolver owner)
    {
        ResolvableType[] result = new ResolvableType[types.length];
        for (int i = 0; i < types.length; i++)
        {
            result[i] = forType(types[i], owner);
        }
        return result;
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class}. For example
     * {@code ResolvableType.forClass(MyArrayList.class)}.
     *
     * @param sourceClass the source class (must not be {@code null}
     * @return a {@link ResolvableType} for the specified class
     * @see #forClass(Class, Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> sourceClass)
    {
        checkArgument(sourceClass != null, "Source class must not be null");
        return forType(sourceClass);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class} with a given
     * implementation. For example
     * {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
     *
     * @param sourceClass         the source class (must not be {@code null}
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified class backed by the given
     * implementation class
     * @see #forClass(Class)
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClass(Class<?> sourceClass, Class<?> implementationClass)
    {
        checkArgument(sourceClass != null, "Source class must not be null");
        ResolvableType asType = forType(implementationClass).as(sourceClass);
        return (asType == NONE ? forType(sourceClass) : asType);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Field}.
     *
     * @param field the source field
     * @return a {@link ResolvableType} for the specified field
     * @see #forField(Field, Class)
     */
    public static ResolvableType forField(Field field)
    {
        checkArgument(field != null, "Field must not be null");
        return forType(null, new FieldTypeProvider(field), null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Field} with a given
     * implementation.
     * <p>Use this variant when the class that declares the field includes generic
     * parameter variables that are satisfied by the implementation class.
     *
     * @param field               the source field
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified field
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, Class<?> implementationClass)
    {
        checkArgument(field != null, "Field must not be null");
        ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Field} with a given
     * implementation.
     * <p>Use this variant when the class that declares the field includes generic
     * parameter variables that are satisfied by the implementation type.
     *
     * @param field              the source field
     * @param implementationType the implementation type
     * @return a {@link ResolvableType} for the specified field
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, ResolvableType implementationType)
    {
        checkArgument(field != null, "Field must not be null");
        implementationType = (implementationType == null ? NONE : implementationType);
        ResolvableType owner = implementationType.as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Field} with the
     * given nesting level.
     *
     * @param field        the source field
     * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
     *                     generic type; etc)
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, int nestingLevel)
    {
        checkArgument(field != null, "Field must not be null");
        return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Field} with a given
     * implementation and the given nesting level.
     * <p>Use this variant when the class that declares the field includes generic
     * parameter variables that are satisfied by the implementation class.
     *
     * @param field               the source field
     * @param nestingLevel        the nesting level (1 for the outer level; 2 for a nested
     *                            generic type; etc)
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified field
     * @see #forField(Field)
     */
    public static ResolvableType forField(Field field, int nestingLevel, Class<?> implementationClass)
    {
        checkArgument(field != null, "Field must not be null");
        ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
        return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Constructor} parameter.
     *
     * @param constructor    the source constructor (must not be {@code null})
     * @param parameterIndex the parameter index
     * @return a {@link ResolvableType} for the specified constructor parameter
     * @see #forConstructorParameter(Constructor, int, Class)
     */
    public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex)
    {
        checkArgument(constructor != null, "Constructor must not be null");
        return forMethodParameter(new MethodParameter(constructor, parameterIndex));
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Constructor} parameter
     * with a given implementation. Use this variant when the class that declares the
     * constructor includes generic parameter variables that are satisfied by the
     * implementation class.
     *
     * @param constructor         the source constructor (must not be {@code null})
     * @param parameterIndex      the parameter index
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified constructor parameter
     * @see #forConstructorParameter(Constructor, int)
     */
    public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
                                                         Class<?> implementationClass)
    {

        checkArgument(constructor != null, "Constructor must not be null");
        MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex);
        methodParameter.setContainingClass(implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Method} return type.
     *
     * @param method the source for the method return type
     * @return a {@link ResolvableType} for the specified method return
     * @see #forMethodReturnType(Method, Class)
     */
    public static ResolvableType forMethodReturnType(Method method)
    {
        checkArgument(method != null, "Method must not be null");
        return forMethodParameter(MethodParameter.forMethodOrConstructor(method, -1));
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Method} return type.
     * Use this variant when the class that declares the method includes generic
     * parameter variables that are satisfied by the implementation class.
     *
     * @param method              the source for the method return type
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified method return
     * @see #forMethodReturnType(Method)
     */
    public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass)
    {
        checkArgument(method != null, "Method must not be null");
        MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, -1);
        methodParameter.setContainingClass(implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Method} parameter.
     *
     * @param method         the source method (must not be {@code null})
     * @param parameterIndex the parameter index
     * @return a {@link ResolvableType} for the specified method parameter
     * @see #forMethodParameter(Method, int, Class)
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(Method method, int parameterIndex)
    {
        checkArgument(method != null, "Method must not be null");
        return forMethodParameter(new MethodParameter(method, parameterIndex));
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
     * given implementation. Use this variant when the class that declares the method
     * includes generic parameter variables that are satisfied by the implementation class.
     *
     * @param method              the source method (must not be {@code null})
     * @param parameterIndex      the parameter index
     * @param implementationClass the implementation class
     * @return a {@link ResolvableType} for the specified method parameter
     * @see #forMethodParameter(Method, int, Class)
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass)
    {
        checkArgument(method != null, "Method must not be null");
        MethodParameter methodParameter = new MethodParameter(method, parameterIndex);
        methodParameter.setContainingClass(implementationClass);
        return forMethodParameter(methodParameter);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link MethodParameter}.
     *
     * @param methodParameter the source method parameter (must not be {@code null})
     * @return a {@link ResolvableType} for the specified method parameter
     * @see #forMethodParameter(Method, int)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter)
    {
        return forMethodParameter(methodParameter, (Type) null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link MethodParameter} with a
     * given implementation type. Use this variant when the class that declares the method
     * includes generic parameter variables that are satisfied by the implementation type.
     *
     * @param methodParameter    the source method parameter (must not be {@code null})
     * @param implementationType the implementation type
     * @return a {@link ResolvableType} for the specified method parameter
     * @see #forMethodParameter(MethodParameter)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter, ResolvableType implementationType)
    {
        checkArgument(methodParameter != null, "MethodParameter must not be null");
        implementationType = (implementationType == null ? forType(methodParameter.getContainingClass()) : implementationType);
        ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
        return forType(null, new MethodParameterTypeProvider(methodParameter),
                       owner.asVariableResolver()).getNested(methodParameter.getNestingLevel(),
                                                             methodParameter.typeIndexesPerLevel);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link MethodParameter},
     * overriding the target type to resolve with a specific given type.
     *
     * @param methodParameter the source method parameter (must not be {@code null})
     * @param targetType      the type to resolve (a part of the method parameter's type)
     * @return a {@link ResolvableType} for the specified method parameter
     * @see #forMethodParameter(Method, int)
     */
    public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType)
    {
        checkArgument(methodParameter != null, "MethodParameter must not be null");
        ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
        return forType(targetType, new MethodParameterTypeProvider(methodParameter),
                       owner.asVariableResolver()).getNested(methodParameter.getNestingLevel(),
                                                             methodParameter.typeIndexesPerLevel);
    }

    /**
     * Return a {@link ResolvableType} as a array of the specified {@code componentType}.
     *
     * @param componentType the component type
     * @return a {@link ResolvableType} as an array of the specified component type
     */
    public static ResolvableType forArrayComponent(ResolvableType componentType)
    {
        checkArgument(componentType != null, "ComponentType must not be null");
        Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
        return new ResolvableType(arrayClass, null, null, componentType);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
     *
     * @param sourceClass the source class
     * @param generics    the generics of the class
     * @return a {@link ResolvableType} for the specific class and generics
     * @see #forClassWithGenerics(Class, ResolvableType...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> sourceClass, Class<?>... generics)
    {
        checkArgument(sourceClass != null, "Source class must not be null");
        checkArgument(generics != null, "Generics must not be null");
        ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
        for (int i = 0; i < generics.length; i++)
        {
            resolvableGenerics[i] = forClass(generics[i]);
        }
        return forClassWithGenerics(sourceClass, resolvableGenerics);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
     *
     * @param sourceClass the source class
     * @param generics    the generics of the class
     * @return a {@link ResolvableType} for the specific class and generics
     * @see #forClassWithGenerics(Class, Class...)
     */
    public static ResolvableType forClassWithGenerics(Class<?> sourceClass, ResolvableType... generics)
    {
        checkArgument(sourceClass != null, "Source class must not be null");
        checkArgument(generics != null, "Generics must not be null");
        TypeVariable<?>[] typeVariables = sourceClass.getTypeParameters();
        return forType(sourceClass, new TypeVariablesVariableResolver(typeVariables, generics));
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type}.
     * Note: The resulting {@link ResolvableType} may not be {@link Serializable}.
     *
     * @param type the source type or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type}
     * @see #forType(Type, ResolvableType)
     */
    public static ResolvableType forType(Type type)
    {
        return forType(type, null, null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
     * owner type. Note: The resulting {@link ResolvableType} may not be {@link Serializable}.
     *
     * @param type  the source type or {@code null}
     * @param owner the owner type used to resolve variables
     * @return a {@link ResolvableType} for the specified {@link Type} and owner
     * @see #forType(Type)
     */
    public static ResolvableType forType(Type type, ResolvableType owner)
    {
        VariableResolver variableResolver = null;
        if (owner != null)
        {
            variableResolver = owner.asVariableResolver();
        }
        return forType(type, variableResolver);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
     * {@link VariableResolver}.
     *
     * @param type             the source type or {@code null}
     * @param variableResolver the variable resolver or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
     */
    static ResolvableType forType(Type type, VariableResolver variableResolver)
    {
        return forType(type, null, variableResolver);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
     * {@link VariableResolver}.
     *
     * @param type             the source type or {@code null}
     * @param typeProvider     the type provider or {@code null}
     * @param variableResolver the variable resolver or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
     */
    static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver)
    {
        if (type == null && typeProvider != null)
        {
            type = SerializableTypeWrapper.forTypeProvider(typeProvider);
        }
        if (type == null)
        {
            return NONE;
        }
        // Check the cache, we may have a ResolvableType that may have already been resolved
        cache.purgeUnreferencedEntries();
        ResolvableType key = new ResolvableType(type, typeProvider, variableResolver);
        ResolvableType resolvableType = cache.get(key);
        if (resolvableType == null)
        {
            resolvableType = new ResolvableType(type, typeProvider, variableResolver, null);
            cache.put(resolvableType, resolvableType);
        }
        return resolvableType;
    }


    /**
     * Strategy interface used to resolve {@link TypeVariable}s.
     */
    static interface VariableResolver extends Serializable
    {

        /**
         * Return the source of the resolver (used for hashCode and equals).
         */
        Object getSource();

        /**
         * Resolve the specified variable.
         *
         * @param variable the variable to resolve
         * @return the resolved variable or {@code null}
         */
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }


    @SuppressWarnings("serial")
    private class DefaultVariableResolver implements VariableResolver
    {

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable)
        {
            return ResolvableType.this.resolveVariable(variable);
        }

        @Override
        public Object getSource()
        {
            return ResolvableType.this;
        }
    }


    @SuppressWarnings("serial")
    private static class TypeVariablesVariableResolver implements VariableResolver
    {

        private final TypeVariable<?>[] typeVariables;

        private final ResolvableType[] generics;

        public TypeVariablesVariableResolver(TypeVariable<?>[] typeVariables, ResolvableType[] generics)
        {
            checkArgument(typeVariables.length == generics.length, "Mismatched number of generics specified");
            this.typeVariables = typeVariables;
            this.generics = generics;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable)
        {
            for (int i = 0; i < this.typeVariables.length; i++)
            {
                if (SerializableTypeWrapper.unwrap(this.typeVariables[i]).equals(
                        SerializableTypeWrapper.unwrap(variable)))
                {
                    return this.generics[i];
                }
            }
            return null;
        }

        @Override
        public Object getSource()
        {
            return this.generics;
        }
    }


    /**
     * Internal helper to handle bounds from {@link WildcardType}s.
     */
    private static class WildcardBounds
    {

        private final Kind kind;

        private final ResolvableType[] bounds;

        /**
         * Internal constructor to create a new {@link WildcardBounds} instance.
         *
         * @param kind   the kind of bounds
         * @param bounds the bounds
         * @see #get(ResolvableType)
         */
        public WildcardBounds(Kind kind, ResolvableType[] bounds)
        {
            this.kind = kind;
            this.bounds = bounds;
        }

        /**
         * Return {@code true} if this bounds is the same kind as the specified bounds.
         */
        public boolean isSameKind(WildcardBounds bounds)
        {
            return this.kind == bounds.kind;
        }

        /**
         * Return {@code true} if this bounds is assignable to all the specified types.
         *
         * @param types the types to test against
         * @return {@code true} if this bounds is assignable to all types
         */
        public boolean isAssignableFrom(ResolvableType... types)
        {
            for (ResolvableType bound : this.bounds)
            {
                for (ResolvableType type : types)
                {
                    if (!isAssignable(bound, type))
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isAssignable(ResolvableType source, ResolvableType from)
        {
            return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
        }

        /**
         * Return the underlying bounds.
         */
        public ResolvableType[] getBounds()
        {
            return this.bounds;
        }

        /**
         * Get a {@link WildcardBounds} instance for the specified type, returning
         * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
         *
         * @param type the source type
         * @return a {@link WildcardBounds} instance or {@code null}
         */
        public static WildcardBounds get(ResolvableType type)
        {
            ResolvableType resolveToWildcard = type;
            while (!(resolveToWildcard.getType() instanceof WildcardType))
            {
                if (resolveToWildcard == NONE)
                {
                    return null;
                }
                resolveToWildcard = resolveToWildcard.resolveType();
            }
            WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
            Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
            Type[] bounds = boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds();
            ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
            for (int i = 0; i < bounds.length; i++)
            {
                resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
            }
            return new WildcardBounds(boundsType, resolvableBounds);
        }

        /**
         * The various kinds of bounds.
         */
        static enum Kind
        {
            UPPER, LOWER
        }
    }

}
