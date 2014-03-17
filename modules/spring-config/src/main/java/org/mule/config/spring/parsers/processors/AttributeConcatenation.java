/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.util.StringUtils;

import org.w3c.dom.Element;

public class AttributeConcatenation implements PreProcessor
{

    private String target;
    private String separator;
    private String[] sources;

    public AttributeConcatenation(String target, String separator, String[] sources)
    {
        this.target = target;
        this.separator = separator;
        this.sources = sources;
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        StringBuilder concat = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < sources.length; ++i)
        {
            String value = config.translateValue(sources[i], element.getAttribute(sources[i])).toString();
            if (StringUtils.isNotEmpty(value))
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    concat.append(separator);
                }
                concat.append(value);
            }
        }
        element.setAttribute(target, concat.toString());
    }

}
