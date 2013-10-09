/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        StringBuffer concat = new StringBuffer();
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
