/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.generics.GenericsUtils.getCollectionType;

import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.StringUtils;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
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
public class DefaultDataTypeBuilder implements DataTypeBuilder, DataTypeBuilder.DataTypeCollectionTypeBuilder {

  private static ConcurrentHashMap<String, ProxyIndicator> proxyClassCache = new ConcurrentHashMap<>();

  private static LoadingCache<DefaultDataTypeBuilder, DataType> dataTypeCache =
      newBuilder().softValues().build(new CacheLoader<DefaultDataTypeBuilder, DataType>() {

        @Override
        public DataType load(DefaultDataTypeBuilder key) throws Exception {
          return key.doBuild();
        }
      });

  private Class<?> type = Object.class;
  private DataTypeBuilder itemTypeBuilder;
  private MediaType mediaType = MediaType.ANY;

  private boolean built = false;

  public DefaultDataTypeBuilder() {

  }

  public DefaultDataTypeBuilder(DataType dataType) {
    if (dataType instanceof CollectionDataType) {
      this.type = dataType.getType();
      this.itemTypeBuilder = DataType.builder(((CollectionDataType) dataType).getItemDataType());
    } else {
      this.type = dataType.getType();
    }
    this.mediaType = dataType.getMediaType();
  }

  /**
   * Sets the given type for the {@link DataType} to be built. See {@link DataType#getType()}.
   * 
   * @param type the java type to set.
   * @return this builder.
   */
  @Override
  public DataTypeParamsBuilder type(Class<?> type) {
    validateAlreadyBuilt();

    checkNotNull(type, "'type' cannot be null.");
    this.type = handleProxy(type);

    return this;
  }

  /*
   * Special case where proxies are used for testing.
   */
  protected Class<?> handleProxy(Class<?> type) {
    if (isProxyClass(type)) {
      return type.getInterfaces()[0];
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

  // TODO MULE-10147 Encapsulate isConsumable logic within DataType
  @Override
  public DataTypeCollectionTypeBuilder streamType(Class<? extends Iterator> iteratorType) {
    validateAlreadyBuilt();

    checkNotNull(iteratorType, "'iteratorType' cannot be null.");
    if (!Iterator.class.isAssignableFrom(iteratorType)) {
      throw new IllegalArgumentException("iteratorType " + iteratorType.getName() + " is not an Iterator type");
    }

    this.type = handleProxy(iteratorType);

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

    this.type = handleProxy(collectionType);

    if (this.itemTypeBuilder == null) {
      this.itemTypeBuilder = DataType.builder();
    }
    final Class<?> itemType = getCollectionType((Class<? extends Iterable<?>>) type);
    if (itemType != null) {
      this.itemTypeBuilder.type(itemType);
    }

    return asCollectionTypeBuilder();
  }

  @Override
  public DataTypeCollectionTypeBuilder asCollectionTypeBuilder() {
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

  /**
   * Sets the given charset. See {@link MediaType#getCharset()}.
   *
   * @param charset the encoding to set.
   * @return this builder.
   */
  @Override
  public DataTypeBuilder charset(String charset) {
    validateAlreadyBuilt();

    if (StringUtils.isNotEmpty(charset)) {
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
    return dataTypeCache.getUnchecked(this);
  }

  protected DataType doBuild() {
    if (Iterable.class.isAssignableFrom(type)) {
      return new DefaultCollectionDataType((Class<? extends Iterable>) type,
                                           itemTypeBuilder != null ? itemTypeBuilder.build() : DataType.OBJECT, mediaType);
    } else {
      return new SimpleDataType(type, mediaType);
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
    return Objects.hash(type, itemTypeBuilder, mediaType);
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

    return Objects.equals(type, other.type) && Objects.equals(itemTypeBuilder, other.itemTypeBuilder)
        && Objects.equals(mediaType, other.mediaType);
  }
}
