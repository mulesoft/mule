/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations;

import org.mule.config.annotations.expressions.Mule;
import org.mule.config.annotations.expressions.XPath;

import org.dom4j.Document;

/**
 * TODO
 */

@Service(name = "aComponent")
public class AnnotatedComponent
{
    @Entrypoint
    public Object doSomething(
            @XPath("/foo/bar") String bar,
            @Mule("message.payload(org.dom4j.Document)") Document doc,
            @Mule("message.header(name)") String name)
    {
        return bar + ":" + name + ":" + doc.asXML();
    }

    public String getSomething()
    {
        return "something";
    }

    public String doSomethingElse(Object something)
    {
        return "somethingElse";
    }
}
