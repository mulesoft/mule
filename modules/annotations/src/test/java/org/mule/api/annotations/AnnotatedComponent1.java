/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations;

import org.mule.api.annotations.expressions.Mule;
import org.mule.api.annotations.expressions.XPath;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;

import org.dom4j.Document;

public class AnnotatedComponent1
{
    public Object doSomething(
            @XPath("/foo/bar") String bar,
            @Payload Document doc,
            @InboundHeaders("name") String name)
    {
        return bar + ":" + name + ":" + doc.asXML();
    }
}
