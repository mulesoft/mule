/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization.internal;

import static com.google.common.primitives.Primitives.isWrapperType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.util.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.lang.SerializationException;
import org.mule.session.DefaultMuleSession;
import org.mule.util.IOUtils;

public class MuleSessionWithNativeTypesSerializer
{

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static byte[] doSerialize(Object object) throws Exception
    {
        if (!(object instanceof DefaultMuleSession))
        {
            throw new InvalidClassException("Only DefaultMuleSession is supported");
        }

        JSONObject jsonMap = new JSONObject();
        DefaultMuleSession session = (DefaultMuleSession) object;
        for (Entry<String, Object> entry : session.getProperties().entrySet())
        {
            Object value = entry.getValue();
            if(value != null)
            {
                if (allowClass(value.getClass()))
                {
                    writeObject(jsonMap, entry.getKey(), value);
                }
                else
                {
                    throw new InvalidClassException("Only primitive types are allowed. "
                            + "Found a '" + value.getClass() +"' for key '" + entry.getKey() + "'");
                }
            }
        }

        return jsonMap.toString().getBytes(UTF_8);
    }

    private static void writeObject(JSONObject jsonMap, String key, Object value)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", value.getClass().getName());
        if (value instanceof Date)
        {
            jsonObject.put("value", dateFormat.format((Date) value));
        }
        else
        {
            jsonObject.put("value", value);
        }
        jsonMap.put(key, jsonObject);
    }

    private static Object readObject(JSONObject jsonMap, String key)
    {
        JSONObject jsonObject = (JSONObject) jsonMap.get(key);
        String typeString = jsonObject.getString("type");
        if (typeString.equals(Date.class.getName()))
        {
            try
            {
                return dateFormat.parse(jsonObject.getString("value"));
            }
            catch (ParseException e)
            {
                throw new SerializationException("Invalid date format processing key '" + key + "': '" + jsonObject.getString("value") + "'");
            }
        }
        else
        {
            return jsonObject.get("value");
        }
    }

    private static <T> T doDeserialize(InputStream inputStream, ClassLoader classLoader) throws Exception
    {
        DefaultMuleSession muleSession = new DefaultMuleSession();

        BufferedReader streamReader = null;
        try
        {
            streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
            {
                responseStrBuilder.append(inputStr);
            }
            
            JSONObject jsonObject = stringToJson(responseStrBuilder.toString());
            
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext())
            {
                String key = keys.next();
                Object value = readObject(jsonObject, key);
                if (allowClass(value.getClass()))
                {
                    muleSession.setProperty(key, value);
                }
                else
                {
                    throw new InvalidClassException("Only primitive types are allowed. "
                            + "Found a '" + value.getClass() + "' for key '" + key + "'");
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(streamReader);
        }

        return (T) muleSession;
    }

    private static boolean allowClass(Class clazz)
    {
        return clazz.isPrimitive() || clazz.equals(String.class) || isWrapperType(clazz) || Date.class.equals(clazz);
    }

    private static JSONObject stringToJson(String asString)
    {
        try
        {
            return new JSONObject(asString);
        }
        catch (JSONException e)
        {
            throw new SerializationException("Invalid JSON '" + asString + "'", e);
        }
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if object is not a {@link java.io.Serializable}
     */
    public static byte[] serialize(Object object) throws SerializationException
    {
        try
        {
            return doSerialize(object);
        }
        catch (Exception e)
        {
            throw new SerializationException("Could not serialize object of class '" + object.getClass().getName() + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public static <T> T deserialize(byte[] bytes, ClassLoader classLoader) throws SerializationException
    {
        checkArgument(bytes != null, "The byte[] must not be null");
        ByteArrayInputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(bytes);
            return deserialize(inputStream, classLoader);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * {@inheritDoc}
     */
    public static <T> T deserialize(InputStream inputStream, ClassLoader classLoader) throws SerializationException
    {
        checkArgument(inputStream != null, "Cannot deserialize a null stream");
        try
        {
            return (T) doDeserialize(inputStream, classLoader);
        }
        catch (Exception e)
        {
            throw new SerializationException("Could not deserialize object", e);
        }
    }
}