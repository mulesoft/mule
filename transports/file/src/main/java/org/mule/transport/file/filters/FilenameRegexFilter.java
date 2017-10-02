/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.filters;

import static java.util.regex.Pattern.COMMENTS; 
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>FilenameRegexFilter</code> filters incoming files from a directory, based
 * on a regular expression. If the expression evaluates to true, then the file will
 * be accepted.
 */
public class FilenameRegexFilter extends FilenameWildcardFilter
{
    protected volatile Pattern[] compiledPatterns = null;

    Pattern regex = Pattern.compile(
            ",         # A comma  \n" +
            "(?!       # not followed...\n" +
            " [^{]*    # by a sequence of characters without an open brance \n" +
            " \\}      # immediately followed by a closing brace\n" +
            ")         # end of lookahead", 
            COMMENTS);

    /**
     * Filter condition decider method.
     * <p>
     * Returns <code>boolean</code> <code>TRUE</code> if the file conforms to the
     * regular expression pattern or <code>FALSE</code> otherwise.
     * 
     * @return indication of acceptance as boolean.
     */
    @Override
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

    @Override
    public void setCaseSensitive(boolean caseSensitive)
    {
        super.setCaseSensitive(caseSensitive);
        this.setPattern(pattern);
    }

    @Override
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
        
        if (pattern == null || pattern.trim().isEmpty())
        {
            this.patterns = new String[0];
        }
        else
        {
            this.patterns = regex.split(pattern);
        }
        

        if (patterns != null)
        {
            compiledPatterns = new Pattern[patterns.length];

            for (int i = 0; i < patterns.length; i++)
            {
                if (!isCaseSensitive())
                {
                    /* Add insensitive option if set in the configuration. */
                    compiledPatterns[i] = Pattern.compile(patterns[i].trim(), Pattern.CASE_INSENSITIVE);
                }
                else
                {
                    compiledPatterns[i] = Pattern.compile(patterns[i].trim());
                }
            }
        }
    }

}
