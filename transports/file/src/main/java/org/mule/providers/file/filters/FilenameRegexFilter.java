/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameRegexFilter extends FilenameWildcardFilter
{
    protected Pattern[] compiledPatterns = null;

    // @Override
    public boolean accept(Object object)
    {
        if (object == null)
        {
            return false;
        }

        boolean foundMatch = false;

        if (compiledPatterns != null)
        {
            for (int i = 0; i < compiledPatterns.length; i++)
            {
                Pattern pattern = compiledPatterns[i];
                String string = object.toString();

                /* Determine if there is an exact match. */
                Matcher matcher = pattern.matcher(string);
                foundMatch = matcher.matches();

                if (foundMatch)
                {
                    // we found a match, bail
                    break;
                }
            }
        }

        return foundMatch;
    }

    // @Override
    public void setCaseSensitive(boolean caseSensitive)
    {
        super.setCaseSensitive(caseSensitive);
        this.setPattern(pattern);
    }

    // @Override
    public void setPattern(String pattern)
    {
        super.setPattern(pattern);

        if (patterns != null)
        {
            compiledPatterns = new Pattern[patterns.length];

            for (int i = 0; i < patterns.length; i++)
            {
                if (!isCaseSensitive())
                {
                    /* Add insensitive option if set in the configuration. */
                    compiledPatterns[i] = Pattern.compile(patterns[i], Pattern.CASE_INSENSITIVE);
                }
                else
                {
                    compiledPatterns[i] = Pattern.compile(patterns[i]);
                }
            }
        }
    }

}
