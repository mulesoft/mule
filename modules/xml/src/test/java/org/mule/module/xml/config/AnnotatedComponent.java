/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.annotations.expression.XPath;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;

public class AnnotatedComponent
{

    public Map<String, Object> doStuff(
            @XPath("/foo") Document fooDocument,
            @XPath("/foo/bar") String bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(2);
        map.put("foo", fooDocument);
        map.put("bar", bar);
        return map;
    }

    public Map<String, Object> doStuffWithBooleanParam(
            @XPath("/foo") Document fooDocument,
            @XPath("/foo/bar == 'barValue'") boolean isBarValue,
            @XPath("/foo/bar") String bar)
    {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("foo", fooDocument);
        map.put("isBarValue", isBarValue);
        map.put("bar", bar);
        return map;
    }
}
