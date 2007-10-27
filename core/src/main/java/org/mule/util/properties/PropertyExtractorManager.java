/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.properties;

import org.mule.config.i18n.CoreMessages;

import java.util.HashMap;
import java.util.Map;

/** TODO */
public class PropertyExtractorManager
{
    public static final String DEFAULT_EXTRACTOR_NAME = MessageHeaderPropertyExtractor.NAME;

    private static String defaultExtractor = DEFAULT_EXTRACTOR_NAME;

    private static Map extractors = new HashMap(8);

    public static void registerExtractor(PropertyExtractor extractor)
    {
        if(extractor ==null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("extractor").getMessage());
        }
        if(extractors.containsKey(extractor.getName()))
        {
            throw new IllegalArgumentException(CoreMessages.objectAlreadyExists(extractor.getName()).getMessage());
        }
        extractors.put(extractor.getName(), extractor);
    }

    public static PropertyExtractor unregisterExtractor(String name)
    {
        if(name==null)
        {
            return null;
        }
        if(name.equals(getDefaultExtractor()))
        {
            setDefaultExtractor(DEFAULT_EXTRACTOR_NAME);
        }
        return (PropertyExtractor)extractors.remove(name);
    }

    public static Object processExpression(String expression, Object object)
    {
        String extractorName = getDefaultExtractor();

        if(expression==null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if(expression.startsWith("${"))
        {
            expression = expression.substring(2, expression.length()-1);
        }
        int i = expression.indexOf(":");
        if(i>-1)
        {
            extractorName = expression.substring(0, i);
            expression = expression.substring(i+1);
        }
        PropertyExtractor extractor = (PropertyExtractor)extractors.get(extractorName);
        if(extractor==null)
        {
            throw new IllegalArgumentException(CoreMessages.noExtractorRegisteredWithKey(extractorName).getMessage());
        }
        return extractor.getProperty(expression, object);
    }

    public static String getDefaultExtractor()
    {
        return defaultExtractor;
    }

    public static void setDefaultExtractor(String defaultExtractor)
    {
        if(extractors.get(defaultExtractor)==null)
        {
            throw new IllegalArgumentException(defaultExtractor);
        }
        PropertyExtractorManager.defaultExtractor = defaultExtractor;
    }

    public static synchronized void clear()
    {
        defaultExtractor = DEFAULT_EXTRACTOR_NAME;        
        extractors.clear();
    }
}
