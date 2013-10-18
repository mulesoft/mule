/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.annotations.expression.XPath;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnnotatedComponent
{
    public Map<String, Object> doStuff(
            @XPath("/foo") Element fooDocument,
            @XPath("/foo/bar[1] = 4") Boolean isBarValue,
            @XPath("/foo/bar[1]") String bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", fooDocument);
        map.put("isBarValue", isBarValue);
        map.put("bar", bar);
        return map;
    }

     public Map<String, Object> doStuff2(
            @XPath("/foo") Document fooDocument,
            @XPath("/foo/bar[2] = '8'") Boolean isBarValue,
            @XPath("/foo/bar[2]") Double bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", fooDocument);
        map.put("isBarValue", isBarValue);
        map.put("bar", bar);
        return map;
    }

    public Map<String, Object> doStuff3(
            @XPath("/foo") Node foo,
            @XPath("/foo/bar") NodeList barNodes)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", foo);
        map.put("bar", barNodes);
        return map;
    }

    public Map<String, Object> doStuff4(@XPath("/faz") Node foo)
    {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("foo", foo);
        return map;
    }

    public Map<String, Object> doStuff5(@XPath(value = "/faz", optional = true) Node foo)
    {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("foo", foo);
        return map;
    }
}
