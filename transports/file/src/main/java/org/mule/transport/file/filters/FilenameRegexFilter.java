/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.filters;

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
