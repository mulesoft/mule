/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
