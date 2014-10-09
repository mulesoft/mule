/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Parses Json payloads into different representations of a
 * Json. Implementations are to be assumed thread-safe
 *
 * @since 3.6.0
 */
public interface JsonParser
{

    /**
     * Parses the given {@code input} into a {@link JsonNode}.
     *
     * @param input the input to be transformed into a {@link JsonNode}
     * @return a {@link JsonNode} if the input could be parsed. {@code null}
     * if it's not possible to transform
     * @throws IOException
     */
    JsonNode asJsonNode(Object input) throws IOException;
}
