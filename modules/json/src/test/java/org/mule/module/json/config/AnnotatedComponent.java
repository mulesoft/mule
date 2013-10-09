/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
