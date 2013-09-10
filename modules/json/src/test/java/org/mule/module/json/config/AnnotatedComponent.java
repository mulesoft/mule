/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.api.annotations.expression.JsonPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

public class AnnotatedComponent
{
    public Map<String, Object> doStuff(
            @JsonPath("/foo") JsonNode fooDocument,
            @JsonPath("/foo/bar[0] = 4") Boolean isBarValue,
            @JsonPath("/foo/bar[0]") String bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", fooDocument);
        map.put("isBarValue", isBarValue);
        map.put("bar", bar);
        return map;
    }

     public Map<String, Object> doStuff2(
            @JsonPath("/foo") JsonNode fooDocument,
            @JsonPath("/foo/bar[1] = 8") Boolean isBarValue,
            @JsonPath("/foo/bar[1]") Double bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", fooDocument);
        map.put("isBarValue", isBarValue);
        map.put("bar", bar);
        return map;
    }

    public Map<String, Object> doStuff3(
            @JsonPath("/foo") JsonNode foo,
            @JsonPath("/foo/bar") List barNodes)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", foo);
        map.put("bar", barNodes);
        return map;
    }

    public Map<String, Object> doStuff4(@JsonPath("/faz") JsonNode foo)
    {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("foo", foo);
        return map;
    }

    public Map<String, Object> doStuff5(@JsonPath(value = "/faz", optional = true) JsonNode foo)
    {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("foo", foo);
        return map;
    }
}
