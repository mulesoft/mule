/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.fruit;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
public class AppleBodyReader implements MessageBodyReader<Apple>
{

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
    {
        return type == Apple.class;
    }

    @Override
    public Apple readFrom(Class<Apple> appleClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> stringStringMultivaluedMap, InputStream inputStream) throws IOException, WebApplicationException
    {
        String[] payload = IOUtils.toString(inputStream).split(":");

        Apple apple = new Apple();
        apple.setBitten(Boolean.valueOf(payload[0]));
        apple.setWashed(Boolean.valueOf(payload[1]));

        return apple;
    }
}
