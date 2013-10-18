/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.routing.filters.RegExFilter;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegExFilterDefinitionParser extends FilterDefinitionParser
{
    public RegExFilterDefinitionParser()
    {
        super(RegExFilter.class);
        addMapping("flags", new FlagsMapping());
    }

    /**
     * Map a comma-separated string to an Integer object that can be used to set the flags
     * on a {@link RegExFilter}.
     */
    public static class FlagsMapping implements ValueMap
    {
        private static final Map<String, Integer> FlagsMapping;

        static
        {
            FlagsMapping = new HashMap<String, Integer>();
            FlagsMapping.put("CANON_EQ", Integer.valueOf(Pattern.CANON_EQ));
            FlagsMapping.put("CASE_INSENSITIVE", Integer.valueOf(Pattern.CASE_INSENSITIVE));
            FlagsMapping.put("DOTALL", Integer.valueOf(Pattern.DOTALL));
            FlagsMapping.put("MULTILINE", Integer.valueOf(Pattern.MULTILINE));
            FlagsMapping.put("UNICODE_CASE", Integer.valueOf(Pattern.UNICODE_CASE));
        }

        public Object rewrite(String value)
        {
            int combinedFlags = 0;

            String[] flagStrings = StringUtils.split(value, ',');
            for (String flagString : flagStrings)
            {
                Integer flag = FlagsMapping.get(flagString);
                if (flag == null)
                {
                    String message = String.format("Invalid flag '%1s'. Must be one of %2s", flagString,
                        FlagsMapping.keySet().toString());
                    throw new IllegalArgumentException(message);
                }

                combinedFlags = combinedFlags | flag.intValue();
            }

            return Integer.valueOf(combinedFlags);
        }
    }
}
