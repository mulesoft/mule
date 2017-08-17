/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.core.internal.util.generics.GenericsUtils.getCollectionType;
import static org.mule.runtime.core.internal.util.generics.GenericsUtils.getMapKeyType;
import static org.mule.runtime.core.internal.util.generics.GenericsUtils.getMapValueType;

import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.runtime.core.api.util.ClassUtils;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.InputStream;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;

/**
 * Provides a way to build immutable {@link DataType} objects.
 *
 * @since 4.0
 */
public class DefaultDataTypeBuilder
    implements DataTypeBuilder, DataTypeBuilder.DataTypeCollectionTypeBuilder, DataTypeBuilder.DataTypeFunctionTypeBuilder,
    DataTypeBuilder.DataTypeMapTypeBuilder {

  private static ConcurrentHashMap<String, ProxyIndicator> proxyClassCache = new ConcurrentHashMap<>();
  private static ConcurrentHashMap<String, ProxyIndicator> cglibClassCache = new ConcurrentHashMap<>();

  private static LoadingCache<DefaultDataTypeBuilder, DataType> dataTypeCache =
      newBuilder().weakValues().build(new CacheLoader<DefaultDataTypeBuilder, DataType>() {

        @Override
        public DataType load(DefaultDataTypeBuilder key) throws Exception {
          return key.doBuild();
        }
      });

  private Reference<Class<?>> typeRef = new WeakReference<>(Object.class);
  private DataTypeBuilder itemTypeBuilder;
  private MediaType mediaType = MediaType.ANY;
  private DataType returnType;
  private List<FunctionParameter> parametersType;
  private DataTypeBuilder keyTypeBuilder;
  private DataTypeBuilder valueTypeBuilder;

  private boolean built = false;

  public DefaultDataTypeBuilder() {

  }

  public DefaultDataTypeBuilder(DataType dataType) {
    if (dataType instanceof CollectionDataType) {
      this.typeRef = new WeakReference<>(dataType.getType());
      this.itemTypeBuilder = DataType.builder(((CollectionDataType) dataType).getItemDataType());
    } else if (dataType instanceof MapDataType) {
      this.typeRef = new WeakReference<>(dataType.getType());
      this.keyTypeBuilder = DataType.builder(((MapDataType) dataType).getKeyDataType());
      this.valueTypeBuilder = DataType.builder(((MapDataType) dataType).getValueDataType());
    } else if (dataType instanceof FunctionDataType) {
      this.typeRef = new WeakReference<>(dataType.getType());
      Optional<DataType> returnType = ((FunctionDataType) dataType).getReturnType();
      if (returnType.isPresent()) {
        this.returnType = returnType.get();
      }
      this.parametersType = ((FunctionDataType) dataType).getParameters();
    } else {
      this.typeRef = new WeakReference<>(dataType.getType());
    }
    this.mediaType = dataType.getMediaType();
  }

  /**
   * Sets the given typeRef for the {@link DataType} to be built. See {@link DataType#getType()}.
   *
   * @param type the java typeRef to set.
   * @return this builder.
   */
  @Override
  public DataTypeParamsBuilder type(Class<?> type) {
    validateAlreadyBuilt();

    checkNotNull(type, "'type' cannot be null.");
    this.typeRef = new WeakReference<>(handleProxy(type));

    return this;
  }

  /*
   * Special case where proxies are used for testing.
   */
  protected Class<?> handleProxy(Class<?> type) {
    if (isProxyClass(type)) {
      return type.getInterfaces()[0];
    } else if (isCglibClass(type)) {
      return type.getSuperclass().equals(Object.class) ? type.getInterfaces()[0] : type.getSuperclass();
    } else {
      return type;
    }
  }

  /**
   * Cache which classes are proxies. Very experimental
   */
  protected static <T> boolean isProxyClass(Class<T> type) {
    String typeName = type.getName();
    ProxyIndicator indicator = proxyClassCache.get(typeName);
    if (indicator != null) {
      Class classInMap = indicator.getTargetClass();
      if (classInMap == type) {
        return indicator.isProxy();
      } else if (classInMap != null) {
        // We have duplicate class names from different active classloaders. Skip the
        // optimization for this one
        return Proxy.isProxyClass(type);
      }
    }
    // Either there's no indicator in the map or there's one that is due to be replaced
    boolean isProxy = Proxy.isProxyClass(type);
    proxyClassCache.put(typeName, new ProxyIndicator(type, isProxy));
    return isProxy;
  }

  /**
   * Cache which classes are generated by CGLib. Very experimental
   */
  protected static <T> boolean isCglibClass(Class<T> type) {
    String typeName = type.getName();
    ProxyIndicator indicator = cglibClassCache.get(typeName);
    if (indicator != null) {
      Class classInMap = indicator.getTargetClass();
      if (classInMap == type) {
        return indicator.isProxy();
      } else if (classInMap != null) {
        // We have duplicate class names from different active classloaders. Skip the
        // optimization for this one
        return type.getName().contains("CGLIB$$");
      }
    }
    // Either there's no indicator in the map or there's one that is due to be replaced
    // Use the approach by name for 2 reasons:
    // * to avoid having a cglib dependency
    // * since many libs shade cglib, this accounts for those
    boolean isProxy = type.getName().contains("CGLIB$$");
    cglibClassCache.put(typeName, new ProxyIndicator(type, isProxy));
    return isProxy;
  }

  /**
   * map value
   */
  private static final class ProxyIndicator {

    private final WeakReference<Class> targetClassRef;
    private final boolean isProxy;

    ProxyIndicator(Class targetClass, boolean proxy) {
      this.targetClassRef = new WeakReference<>(targetClass);
      isProxy = proxy;
    }

    public Class getTargetClass() {
      return targetClassRef.get();
    }

    public boolean isProxy() {
      return isProxy;
    }
  }

  @Override
  public DataTypeCollectionTypeBuilder streamType(Class<? extends Iterator> iteratorType) {
    validateAlreadyBuilt();

    checkNotNull(iteratorType, "'iteratorType' cannot be null.");
    if (!Iterator.class.isAssignableFrom(iteratorType)) {
      throw new IllegalArgumentException("iteratorType " + iteratorType.getName() + " is not an Iterator type");
    }

    this.typeRef = new WeakReference<>(handleProxy(iteratorType));

    if (this.itemTypeBuilder == null) {
      this.itemTypeBuilder = DataType.builder();
    }

    return asCollectionTypeBuilder();
  }

  /**
   * Sets the given type for the {@link DefaultCollectionDataType} to be built. See {@link DefaultCollectionDataType#getType()}.
   *
   * @param collectionType the java collection type to set.
   * @return this builder.
   * @throws IllegalArgumentException if the given collectionType is not a descendant of {@link Collection}.
   */
  @Override
  public DataTypeCollectionTypeBuilder collectionType(Class<? extends Collection> collectionType) {
    validateAlreadyBuilt();

    checkNotNull(collectionType, "'collectionType' cannot be null.");
    if (!Collection.class.isAssignableFrom(collectionType)) {
      throw new IllegalArgumentException("collectionType " + collectionType.getName() + " is not a Collection type");
    }

    this.typeRef = new WeakReference<>(handleProxy(collectionType));

    if (this.itemTypeBuilder == null) {
      this.itemTypeBuilder = DataType.builder();
    }
    final Class<?> itemType = getCollectionType((Class<? extends Iterable<?>>) typeRef.get());
    if (itemType != null) {
      this.itemTypeBuilder.type(itemType);
    }

    return asCollectionTypeBuilder();
  }

  @Override
  public DataTypeCollectionTypeBuilder asCollectionTypeBuilder() {
    return this;
  }

  @Override
  public DataTypeFunctionTypeBuilder functionType(Class<? extends ExpressionFunction> functionType) {
    validateAlreadyBuilt();

    checkNotNull(functionType, "'functionType' cannot be null.");
    if (!ExpressionFunction.class.isAssignableFrom(functionType)) {
      throw new IllegalArgumentException("functionType " + functionType.getName() + " is not an ExpressionFunction type");
    }

    this.typeRef = new WeakReference<>(handleProxy(functionType));

    return asFunctionTypeBuilder();
  }

  @Override
  public DataTypeFunctionTypeBuilder asFunctionTypeBuilder() {
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder mapType(Class<? extends Map> mapType) {
    validateAlreadyBuilt();

    checkNotNull(mapType, "'mapType' cannot be null.");
    if (!Map.class.isAssignableFrom(mapType)) {
      throw new IllegalArgumentException("mapType " + mapType.getName() + " is not a Map type");
    }

    this.typeRef = new WeakReference<>(handleProxy(mapType));

    if (this.keyTypeBuilder == null) {
      this.keyTypeBuilder = DataType.builder();
    }
    final Class<?> keyType = getMapKeyType((Class<? extends Map<?, ?>>) typeRef.get());
    if (keyType != null) {
      this.keyTypeBuilder.type(keyType);
    }
    if (this.valueTypeBuilder == null) {
      this.valueTypeBuilder = DataType.builder();
    }
    final Class<?> valueType = getMapValueType((Class<? extends Map<?, ?>>) typeRef.get());
    if (valueType != null) {
      this.valueTypeBuilder.type(valueType);
    }

    return asMapTypeBuilder();
  }

  @Override
  public DataTypeMapTypeBuilder asMapTypeBuilder() {
    return this;
  }

  /**
   * Sets the given types for the {@link DefaultCollectionDataType} to be built. See {@link DefaultCollectionDataType#getType()}
   * and {@link DefaultCollectionDataType#getItemDataType()}.
   *
   * @param itemType the java type to set.
   * @return this builder.
   * @throws IllegalArgumentException if the given collectionType is not a descendant of {@link Iterable}.
   */
  @Override
  public DataTypeCollectionTypeBuilder itemType(Class<?> itemType) {
    validateAlreadyBuilt();

    checkNotNull(itemType, "'itemTypeBuilder' cannot be null.");

    if (this.itemTypeBuilder == null) {
      this.itemTypeBuilder = DataType.builder();
    }
    this.itemTypeBuilder.type(handleProxy(itemType));
    return this;
  }

  @Override
  public DataTypeFunctionTypeBuilder returnType(DataType dataType) {
    this.returnType = dataType;
    return this;
  }

  @Override
  public DataTypeFunctionTypeBuilder parametersType(List<FunctionParameter> list) {
    this.parametersType = list;
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder keyType(Class<?> keyType) {
    validateAlreadyBuilt();

    checkNotNull(keyType, "'keyType' cannot be null.");

    if (this.keyTypeBuilder == null) {
      this.keyTypeBuilder = DataType.builder();
    }
    this.keyTypeBuilder.type(handleProxy(keyType));
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder valueType(Class<?> valueType) {
    validateAlreadyBuilt();

    checkNotNull(valueType, "'valueType' cannot be null.");

    if (this.valueTypeBuilder == null) {
      this.valueTypeBuilder = DataType.builder();
    }
    this.valueTypeBuilder.type(handleProxy(valueType));
    return this;
  }

  /**
   * Sets the given mediaType string. See {@link DataType#getMediaType()}.
   * <p>
   * If the media type for the given string has a {@code charset} parameter, that will be set as the charset for the
   * {@link DataType} being built. That charset can be overridden by calling {@link #charset(String)}.
   *
   * @param mediaType the media type string to set
   * @return this builder.
   * @throws IllegalArgumentException if the given media type string is invalid.
   */
  @Override
  public DataTypeBuilder mediaType(String mediaType) {
    requireNonNull(mediaType);
    validateAlreadyBuilt();

    this.mediaType = MediaType.parse(mediaType);
    return this;
  }

  @Override
  public DataTypeBuilder mediaType(MediaType mediaType) {
    requireNonNull(mediaType);
    validateAlreadyBuilt();

    this.mediaType = mediaType;
    return this;
  }

  @Override
  public DataTypeCollectionTypeBuilder itemMediaType(String itemMimeType) {
    validateAlreadyBuilt();

    itemTypeBuilder.mediaType(itemMimeType);
    return this;
  }

  @Override
  public DataTypeCollectionTypeBuilder itemMediaType(MediaType itemMediaType) {
    validateAlreadyBuilt();

    itemTypeBuilder.mediaType(itemMediaType);
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder keyMediaType(String keyMediaType) {
    validateAlreadyBuilt();

    keyTypeBuilder.mediaType(keyMediaType);
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder keyMediaType(MediaType keyMediaType) {
    validateAlreadyBuilt();

    keyTypeBuilder.mediaType(keyMediaType);
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder valueMediaType(String valueMediaType) {
    validateAlreadyBuilt();

    valueTypeBuilder.mediaType(valueMediaType);
    return this;
  }

  @Override
  public DataTypeMapTypeBuilder valueMediaType(MediaType valueMediaType) {
    validateAlreadyBuilt();

    valueTypeBuilder.mediaType(valueMediaType);
    return this;
  }

  /**
   * Sets the given charset. See {@link MediaType#getCharset()}.
   *
   * @param charset the encoding to set.
   * @return this builder.
   */
  @Override
  public DataTypeBuilder charset(String charset) {
    validateAlreadyBuilt();

    if (!isEmpty(charset)) {
      mediaType = mediaType.withCharset(Charset.forName(charset));
    } else {
      mediaType = mediaType.withCharset(null);
    }
    return this;
  }

  @Override
  public DataTypeBuilder charset(Charset charset) {
    validateAlreadyBuilt();

    mediaType = mediaType.withCharset(charset);
    return this;
  }

  @Override
  public DataTypeParamsBuilder fromObject(Object value) {
    validateAlreadyBuilt();

    if (value == null) {
      return type(Object.class);
    } else {
      DataTypeBuilder builder = (DataTypeBuilder) type(value.getClass());
      return getObjectMimeType(value).map(mediaType -> builder.mediaType(mediaType)).orElse(builder);
    }
  }

  @Override
  public DataTypeFunctionTypeBuilder fromFunction(ExpressionFunction expressionFunction) {
    return functionType(expressionFunction.getClass())
        .returnType(expressionFunction.returnType().orElse(null))
        .parametersType(expressionFunction.parameters());
  }

  private Optional<String> getObjectMimeType(Object value) {
    if (value instanceof DataHandler) {
      return of(((DataHandler) value).getContentType());
    } else if (value instanceof DataSource) {
      return of(((DataSource) value).getContentType());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Builds a new {@link DataType} with the values set in this builder.
   *
   * @return a newly built {@link DataType}.
   */
  @Override
  public DataType build() {
    if (built) {
      throwAlreadyBuilt();
    }

    built = true;
    Class<?> type = this.typeRef.get();
    if (ExpressionFunction.class.isAssignableFrom(type)) {
      return new DefaultFunctionDataType(type, returnType, parametersType != null ? parametersType : newArrayList(), mediaType,
                                         isConsumable(type));
    }
    return dataTypeCache.getUnchecked(this);
  }

  protected DataType doBuild() {
    Class<?> type = this.typeRef.get();
    if (Collection.class.isAssignableFrom(type) || Iterator.class.isAssignableFrom(type)) {
      return new DefaultCollectionDataType(type, itemTypeBuilder != null ? itemTypeBuilder.build() : DataType.OBJECT, mediaType,
                                           isConsumable(type));
    } else if (Map.class.isAssignableFrom(type)) {
      return new DefaultMapDataType(type, keyTypeBuilder != null ? keyTypeBuilder.build() : DataType.OBJECT,
                                    valueTypeBuilder != null ? valueTypeBuilder.build() : DataType.OBJECT, mediaType,
                                    isConsumable(type));
    } else {
      return new SimpleDataType(type, mediaType, isConsumable(type));
    }
  }

  protected void validateAlreadyBuilt() {
    if (built) {
      throwAlreadyBuilt();
    }
  }

  protected void throwAlreadyBuilt() {
    throw new IllegalStateException("DataType was already built from this builder. Reusing builder instances is not allowed.");
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeRef.get(), itemTypeBuilder, keyTypeBuilder, valueTypeBuilder, returnType, parametersType, mediaType);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    DefaultDataTypeBuilder other = (DefaultDataTypeBuilder) obj;

    return Objects.equals(typeRef.get(), other.typeRef.get()) && Objects.equals(itemTypeBuilder, other.itemTypeBuilder)
        && Objects.equals(keyTypeBuilder, other.keyTypeBuilder) && Objects.equals(valueTypeBuilder, other.valueTypeBuilder)
        && Objects.equals(returnType, other.returnType) && Objects.equals(parametersType, other.parametersType)
        && Objects.equals(mediaType, other.mediaType);
  }

  private static final List<Class<?>> consumableClasses = new ArrayList<>();

  static {
    addToConsumableClasses("javax.xml.stream.XMLStreamReader");
    addToConsumableClasses("javax.xml.transform.stream.StreamSource");
    consumableClasses.add(OutputHandler.class);
    consumableClasses.add(InputStream.class);
    consumableClasses.add(Reader.class);
    consumableClasses.add(Iterator.class);
  }

  private static void addToConsumableClasses(String className) {
    try {
      consumableClasses.add(ClassUtils.loadClass(className, Message.class));
    } catch (ClassNotFoundException e) {
      // ignore
    }
  }

  /**
   * Determines if the payload of this message is consumable i.e. it can't be read more than once.
   */
  public static boolean isConsumable(Class<?> payloadClass) {
    if (consumableClasses.isEmpty()) {
      return false;
    }

    for (Class<?> c : consumableClasses) {
      if (c.isAssignableFrom(payloadClass)) {
        return true;
      }
    }
    return false;
  }

}
