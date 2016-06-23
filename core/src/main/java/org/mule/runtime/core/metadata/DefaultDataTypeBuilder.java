/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.generics.GenericsUtils.getCollectionType;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MimeType;
import org.mule.runtime.core.util.StringUtils;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;

/**
 * Provides a way to build immutable {@link DataType} objects.
 *
 * @since 4.0
 */
public class DefaultDataTypeBuilder<T> implements DataTypeBuilder<T>, DataTypeBuilder.DataTypeCollectionTypeBuilder<T>
{
    private static ConcurrentHashMap<String, ProxyIndicator> proxyClassCache = new ConcurrentHashMap<>();

    private static LoadingCache<DefaultDataTypeBuilder, DataType> dataTypeCache = newBuilder().softValues().build(new CacheLoader<DefaultDataTypeBuilder, DataType>()
    {
        @Override
        public DataType load(DefaultDataTypeBuilder key) throws Exception
        {
            return key.doBuild();
        }
    });

    private static final String CHARSET_PARAM = "charset";

    private Class<T> type = (Class<T>) Object.class;
    private Class<?> itemType = Object.class;
    private String mimeType = MimeType.ANY;
    private String encoding = null;

    private boolean built = false;

    public DefaultDataTypeBuilder()
    {

    }

    public DefaultDataTypeBuilder(DataType dataType)
    {
        if (dataType instanceof CollectionDataType)
        {
            this.type = dataType.getType();
            this.itemType = ((CollectionDataType) dataType).getItemType();
        }
        else
        {
            this.type = dataType.getType();
        }

        this.mimeType = dataType.getMimeType();
        this.encoding = dataType.getEncoding();
    }

    /**
     * Sets the given type for the {@link DataType} to be built. See {@link DataType#getType()}.
     * 
     * @param type the java type to set.
     * @return this builder.
     */
    @Override
    public <N> DataTypeParamsBuilder<N> type(Class<N> type)
    {
        validateAlreadyBuilt();

        checkNotNull(type, "'type' cannot be null.");
        this.type = (Class<T>) handleProxy(type);

        return (DataTypeParamsBuilder<N>) this;
    }

    /*
     * Special case where proxies are used for testing.
     */
    protected Class<?> handleProxy(Class<?> type)
    {
        if (isProxyClass(type))
        {
            return type.getInterfaces()[0];
        }
        else
        {
            return type;
        }
    }

    /**
     * Cache which classes are proxies. Very experimental
     */
    protected static <T> boolean isProxyClass(Class<T> type)
    {
        String typeName = type.getName();
        ProxyIndicator indicator = proxyClassCache.get(typeName);
        if (indicator != null)
        {
            Class classInMap = indicator.getTargetClass();
            if (classInMap == type)
            {
                return indicator.isProxy();
            }
            else if (classInMap != null)
            {
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
    private static final class ProxyIndicator
    {
        private final WeakReference<Class> targetClassRef;
        private final boolean isProxy;

        ProxyIndicator(Class targetClass, boolean proxy)
        {
            this.targetClassRef = new WeakReference<Class>(targetClass);
            isProxy = proxy;
        }

        public Class getTargetClass()
        {
            return targetClassRef.get();
        }

        public boolean isProxy()
        {
            return isProxy;
        }
    }

    /**
     * Sets the given type for the {@link CollectionDataType} to be built. See
     * {@link CollectionDataType#getType()}.
     * 
     * @param collectionType the java collection type to set.
     * @return this builder.
     * @throws IllegalArgumentException if the given collectionType is not a descendant of
     *             {@link Collection}.
     */
    @Override
    public <N extends Collection> DataTypeCollectionTypeBuilder<N> collectionType(Class<N> collectionType)
    {
        validateAlreadyBuilt();

        checkNotNull(collectionType, "'collectionType' cannot be null.");
        if (!Collection.class.isAssignableFrom(collectionType))
        {
            throw new IllegalArgumentException("collectionType " + collectionType.getName() + " is not a Collection type");
        }

        this.type = (Class<T>) handleProxy(collectionType);
        return (DataTypeCollectionTypeBuilder<N>) this;
    }

    /**
     * Sets the given types for the {@link CollectionDataType} to be built. See
     * {@link CollectionDataType#getType()} and {@link CollectionDataType#getItemType()}.
     * 
     * @param itemType the java type to set.
     * @return this builder.
     * @throws IllegalArgumentException if the given collectionType is not a descendant of {@link Collection}.
     */
    @Override
    public <I> DataTypeParamsBuilder<T> itemType(Class<I> itemType)
    {
        validateAlreadyBuilt();

        checkNotNull(itemType, "'itemType' cannot be null.");

        this.itemType = handleProxy(itemType);
        return this;
    }

    /**
     * Sets the given mimeType string. See {@link DataType#getMimeType()}.
     * <p>
     * If the MIME type for the given string has a {@code charset} parameter, that will be set as
     * the encoding for the {@link DataType} being built. That encoding can be overridden by calling
     * {@link #encoding(String)}.
     * 
     * @param mimeType the MIME type string to set
     * @return this builder.
     * @throws IllegalArgumentException if the given MIME type string is invalid.
     */
    @Override
    public DataTypeBuilder<T> mimeType(String mimeType) throws IllegalArgumentException
    {
        validateAlreadyBuilt();

        if (mimeType == null)
        {
            this.mimeType = MimeType.ANY;
        }
        else
        {
            try
            {
                javax.activation.MimeType mt = new javax.activation.MimeType(mimeType);
                mimeType(mt);
            }
            catch (MimeTypeParseException e)
            {
                throw new IllegalArgumentException("MimeType cannot be parsed: " + mimeType);
            }
        }
        return this;
    }

    @Override
    public DataTypeBuilder<T> mimeType(javax.activation.MimeType mimeType)
    {
        this.mimeType = mimeType.getPrimaryType() + "/" + mimeType.getSubType();

        if (encoding == null && mimeType.getParameter(CHARSET_PARAM) != null)
        {
            encoding = mimeType.getParameter(CHARSET_PARAM);
        }

        return this;
    }

    /**
     * Sets the given encoding. See {@link DataType#getEncoding()}.
     * 
     * @param encoding the encoding to set.
     * @return this builder.
     */
    @Override
    public DataTypeBuilder<T> encoding(String encoding) throws IllegalCharsetNameException
    {
        validateAlreadyBuilt();

        if (StringUtils.isNotEmpty(encoding))
        {
            // Checks that the encoding is valid and supported
            Charset.forName(encoding);
            this.encoding = encoding;
        }

        return this;
    }

    @Override
    public DataTypeBuilder<T> fromObject(T value)
    {
        validateAlreadyBuilt();

        if(value == null)
        {
            return (DataTypeBuilder<T>) type(Object.class).mimeType(MimeType.ANY);
        }
        else
        {
            return (DataTypeBuilder<T>) type(value.getClass()).mimeType(getObjectMimeType(value));
        }
    }

    private static String getObjectMimeType(Object value)
    {
        String mime = null;
        if (value instanceof DataHandler)
        {
            mime = ((DataHandler) value).getContentType();
        }
        else if (value instanceof DataSource)
        {
            mime = ((DataSource) value).getContentType();
        }

        return mime;
    }

    /**
     * Builds a new {@link DataType} with the values set in this builder.
     * 
     * @return a newly built {@link DataType}.
     */
    @Override
    public DataType<T> build()
    {
        if (built)
        {
            throwAlreadyBuilt();
        }

        built = true;
        return dataTypeCache.getUnchecked(this);
    }

    protected DataType<T> doBuild()
    {
        if (Collection.class.isAssignableFrom(type))
        {
            if (itemType == null)
            {
                itemType = getCollectionType((Class<? extends Collection<?>>) type);
            }

            // TODO MULE-9958 provide a default encoding it the builder has null
            return new CollectionDataType(type, itemType, mimeType, encoding);
        }
        else
        {
            // TODO MULE-9958 provide a default encoding it the builder has null
            return new SimpleDataType<>(type, mimeType, encoding);
        }
    }

    protected void validateAlreadyBuilt()
    {
        if (built)
        {
            throwAlreadyBuilt();
        }
    }

    protected void throwAlreadyBuilt()
    {
        throw new IllegalStateException("DataType was already built from this builder. Reusing builder instances is not allowed.");
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, itemType, mimeType, encoding);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }
        if (obj.getClass() != getClass())
        {
            return false;
        }
        DefaultDataTypeBuilder other = (DefaultDataTypeBuilder) obj;

        return Objects.equals(type, other.type)
               && Objects.equals(itemType, other.itemType)
               && Objects.equals(mimeType, other.mimeType)
               && Objects.equals(encoding, other.encoding);
    }
}
