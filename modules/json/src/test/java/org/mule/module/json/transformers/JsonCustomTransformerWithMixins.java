/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;


import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.tck.testmodels.fruit.Apple;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.annotation.PostConstruct;

@ContainsTransformerMethods
public class JsonCustomTransformerWithMixins
{
    private ObjectMapper mapper;

    @PostConstruct
    public void init()
    {
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(Apple.class, AppleMixin.class);
        mapper.getDeserializationConfig().addMixInAnnotations(Apple.class, AppleMixin.class);
    }

    @Transformer(sourceTypes = {InputStream.class, byte[].class})
    public Apple toApple(String in) throws IOException
    {
        return mapper.readValue(in, Apple.class);
    }

    @Transformer
    public String fromApple(Apple apple) throws IOException
    {
        StringWriter w = new StringWriter();
        mapper.writeValue(w, apple);
        return w.toString();

    }

}
