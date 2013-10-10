/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
