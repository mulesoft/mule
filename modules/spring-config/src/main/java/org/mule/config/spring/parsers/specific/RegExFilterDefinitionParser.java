/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
