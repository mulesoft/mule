/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.serialization.internal.MuleSessionWithNativeTypesSerializer;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.lang.SerializationException;

public class SerializationUtils extends org.apache.commons.lang.SerializationUtils
{
    public static Object deserialize(InputStream inputStream, MuleContext muleContext)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException("The MuleContext must not be null");
        }
        return deserialize(inputStream, muleContext.getExecutionClassLoader(), muleContext);
    }

    public static Object deserialize(byte[] objectData, MuleContext muleContext)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException("The MuleContext must not be null");
        }
        return deserialize(objectData, muleContext.getExecutionClassLoader(), muleContext);
    }

    public static Object deserialize(byte[] objectData, MuleContext muleContext,
            ObjectInputStreamProvider objectInputStreamProvider, boolean nativeSerializationActivated)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException("The MuleContext must not be null");
        }
        
        if(nativeSerializationActivated)
        {
            return MuleSessionWithNativeTypesSerializer.deserialize(objectData, muleContext.getExecutionClassLoader());
        }
        else
        {
            return deserialize(objectData, muleContext.getExecutionClassLoader(), muleContext, objectInputStreamProvider);
        }
    }
    
    public static Object deserialize(byte[] objectData, MuleContext muleContext,
                                     ObjectInputStreamProvider objectInputStreamProvider)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException("The MuleContext must not be null");
        }
        return deserialize(objectData, muleContext.getExecutionClassLoader(), muleContext, objectInputStreamProvider);
    }
    
    /**
     * <p>Deserializes an <code>Object</code> from the specified stream.</p>
     * <p/>
     * <p>The stream will be closed once the object is written. This
     * avoids the need for a finally clause, and maybe also exception
     * handling, in the application code.</p>
     * <p/>
     * <p>The stream passed in is not buffered internally within this method.
     * This is the responsibility of your application if desired.</p>
     *
     * @param inputStream the serialized object input stream, must not be null
     * @param cl          classloader which can load custom classes from the stream
     * @param muleContext mule context
     * @param objectInputStreamProvider to provide the {@link ObjectInputStream} used for deserialization
     * @return the deserialized object
     * @throws IllegalArgumentException if <code>inputStream</code> is <code>null</code>
     * @throws org.apache.commons.lang.SerializationException
     *                                  (runtime) if the serialization fails
     */
    public static Object deserialize(InputStream inputStream, ClassLoader cl, MuleContext muleContext,
                                     ObjectInputStreamProvider objectInputStreamProvider)
    {
        if (inputStream == null)
        {
            throw new IllegalArgumentException("The InputStream must not be null");
        }
        if (cl == null)
        {
            throw new IllegalArgumentException("The ClassLoader must not be null");
        }
        ObjectInputStream in = null;
        try
        {
            // stream closed in the finally
            in = objectInputStreamProvider.get(cl, inputStream);
            Object obj = in.readObject();
            if (obj instanceof DeserializationPostInitialisable)
            {
                DeserializationPostInitialisable.Implementation.init(obj, muleContext);
            }
            return obj;
        }
        catch (ClassNotFoundException ex)
        {
            throw new SerializationException(ex);
        }
        catch (IOException ex)
        {
            throw new SerializationException(ex);
        }
        catch (Exception ex)
        {
            throw new SerializationException(ex);
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            }
            catch (IOException ex)
            {
                // ignore close exception
            }
        }
    }


    /**
     * <p>Deserializes an <code>Object</code> from the specified stream.</p>
     * <p/>
     * <p>The stream will be closed once the object is written. This
     * avoids the need for a finally clause, and maybe also exception
     * handling, in the application code.</p>
     * <p/>
     * <p>The stream passed in is not buffered internally within this method.
     * This is the responsibility of your application if desired.</p>
     *
     * @param inputStream the serialized object input stream, must not be null
     * @param cl          classloader which can load custom classes from the stream
     * @param muleContext mule context
     * @return the deserialized object
     * @throws IllegalArgumentException if <code>inputStream</code> is <code>null</code>
     * @throws org.apache.commons.lang.SerializationException
     *                                  (runtime) if the serialization fails
     */
    public static Object deserialize(InputStream inputStream, ClassLoader cl, MuleContext muleContext)
    {
        return deserialize(inputStream, cl, muleContext, new ObjectInputStreamProvider()
        {
            @Override
            public ObjectInputStream get(ClassLoader classLoader, InputStream inputStream) throws IOException
            {
                return new ClassLoaderObjectInputStream(classLoader, inputStream);
            }
        });
    }

    /**
     * <p>Deserializes a single <code>Object</code> from an array of bytes.</p>
     *
     * @param objectData the serialized object, must not be null
     * @param cl         classloader which can load custom classes from the stream
     * @return the deserialized object
     * @throws IllegalArgumentException if <code>objectData</code> is <code>null</code>
     * @throws SerializationException   (runtime) if the serialization fails
     */
    private static Object deserialize(byte[] objectData, ClassLoader cl, MuleContext muleContext)
    {
        if (objectData == null)
        {
            throw new IllegalArgumentException("The byte[] must not be null");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
        return deserialize(bais, cl, muleContext);
    }

    private static Object deserialize(byte[] objectData, ClassLoader cl, MuleContext muleContext,
                                      ObjectInputStreamProvider objectInputStreamProvider)
    {
        if (objectData == null)
        {
            throw new IllegalArgumentException("The byte[] must not be null");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
        return deserialize(bais, cl, muleContext, objectInputStreamProvider);
    }

    /**
     * @deprecated Call deserialize(InputStream inputStream, MuleContext muleContext) instead
     */
    public static Object deserialize(InputStream inputStream, ClassLoader cl)
    {
        return deserialize(inputStream, cl, null);
    }
    
    /**
     * @deprecated Call deserialize(byte[] objectData, MuleContext muleContext) instead
     */
    public static Object deserialize(byte[] objectData, ClassLoader cl)
    {
        return deserialize(objectData, cl, null);
    }    
    
    /**
     * <p>Serializes an <code>Object</code> to a byte array for
     * storage/serialization.</p>
     *
     * @param obj  the object to serialize to bytes
     * @return a byte[] with the converted Serializable
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static byte[] serialize(Serializable obj, boolean nativeSerializationActivated) {
        if(nativeSerializationActivated)
        {
            return MuleSessionWithNativeTypesSerializer.serialize(obj);
        }
        else
        {
            return serialize(obj);
        }
    }
    
}
