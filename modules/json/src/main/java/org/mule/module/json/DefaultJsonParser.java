/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.api.MuleContext;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.google.common.base.Joiner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link JsonParser}.
 */
public final class DefaultJsonParser implements JsonParser
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJsonParser.class);
    private static final DataType<?>[] TRANSFORMABLE_SUPPORTED_TYPES = new DataType<?>[] {
            DataTypeFactory.create(JsonData.class),
            DataTypeFactory.create(JsonNode.class),
            DataTypeFactory.create(String.class)
    };
    private static final String TRANSFORMABLE_SUPPORTED_TYPES_AS_STRING = Joiner.on(',').join(TRANSFORMABLE_SUPPORTED_TYPES);

    private final MuleContext muleContext;

    public DefaultJsonParser(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     * This implementation is capable of handling inputs of these types:
     * <li>
     * <ul>{@link String}</ul>
     * <ul>{@link Reader}</ul>
     * <ul>{@link InputStream}</ul>
     * <ul>{@link byte[]}</ul>
     * <ul>{@link JsonNode}</ul>
     * <ul>{@link JsonData}</ul>
     * </li>
     * <p/>
     * If {@code input} is not of any of those types, then this parser
     * will try to locate a {@link Transformer} capable of transforming
     * it into any of the following types (in the listed order):
     * <p/>
     * <li>
     * <ul>{@link JsonData}</ul>
     * <ul>{@link JsonNode}</ul>
     * <ul>{@link String}</ul>
     * </li>
     * <p/>
     * If no {@link Transformer} is found, then {@code null} is returned.
     */
    @Override
    public JsonNode asJsonNode(Object input) throws IOException
    {
        JsonNode jsonNode = toJsonNode(input);
        if (jsonNode == null)
        {
            LOGGER.debug("Input type {} was not of any supported type. Attempting with transformer resolution of the following types {}",
                         input.getClass().getName(), TRANSFORMABLE_SUPPORTED_TYPES_AS_STRING);

            input = TransformerUtils.transformToAny(input, muleContext, TRANSFORMABLE_SUPPORTED_TYPES);
            jsonNode = asJsonNode(input);

            if (jsonNode == null)
            {
                LOGGER.debug("Could not transform input of type {} to any supported type. Returning null", input.getClass().getName());
            }
        }

        return jsonNode;
    }

    private JsonNode toJsonNode(Object input) throws IOException
    {
        if (input instanceof String)
        {
            return JsonLoader.fromString((String) input);
        }
        else if (input instanceof Reader)
        {
            return JsonLoader.fromReader((Reader) input);
        }
        else if (input instanceof InputStream)
        {
            return JsonLoader.fromReader(new InputStreamReader((InputStream) input));
        }
        else if (input instanceof byte[])
        {
            return JsonLoader.fromReader(new InputStreamReader(new ByteArrayInputStream((byte[]) input)));
        }
        else if (input instanceof JsonNode)
        {
            return (JsonNode) input;
        }
        else if (input instanceof JsonData)
        {
            JsonData jsonData = (JsonData) input;
            return JsonLoader.fromReader(new StringReader(jsonData.toString()));
        }

        return null;
    }

}
