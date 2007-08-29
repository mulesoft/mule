/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.w3c.dom.Element;

public class AutoIdUtils
{

    public static final String ATTRIBUTE_ID = AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID;
    private static final AtomicInteger counter = new AtomicInteger(0);
    public static final String ID_PREFIX = "autogenInheritId";

    public static void ensureUniqueId(Element element)
    {
        if (null != element && !element.hasAttribute(ATTRIBUTE_ID))
        {
            element.setAttribute(ATTRIBUTE_ID, ID_PREFIX + counter.incrementAndGet());
        }
    }

}
