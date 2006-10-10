/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.DateUtils;
import org.mule.util.TemplateParser;
import org.mule.util.UUID;

/**
 * <code>SimpleFilenameParser</code> understands a limited set of tokens, namely
 * <ul>
 * <li>${DATE} : the currrent date in the format dd-MM-yy_HH-mm-ss.SS</li>
 * <li>${DATE:yy-MM-dd} : the current date using the specified format</li>
 * <li>${SYSTIME} : The current system time milliseconds</li>
 * <li>${UUID} : A generated Universally unique id</li>
 * <li>${ORIGINALNAME} : The origial file name if the file being written was read
 * from another location</li>
 * <li>${COUNT} : An incremental counter</li>
 * <li>${<Message Property Name>} : A name of a property on the message</li>
 * </ul>
 * Note that square brackets can be used instead of curl brackets, this is useful
 * when defining the file output pattern in a Mule Url endpointUri where the curl
 * bracket is an invalid character.
 */

public class SimpleFilenameParser implements FilenameParser
{
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy_HH-mm-ss.SSS";

    private AtomicLong count = new AtomicLong(0);

    private TemplateParser antParser = TemplateParser.createAntStyleParser();
    private TemplateParser squareParser = TemplateParser.createSquareBracesStyleParser();

    public String getFilename(UMOMessageAdapter adapter, String pattern)
    {
        if (pattern == null)
        {
            return System.currentTimeMillis() + ".dat";
        }
        else
        {
            String result;
            if (pattern.indexOf('{') > -1)
            {
                result = getFilename(adapter, pattern, antParser);
            }
            else
            {
                result = getFilename(adapter, pattern, squareParser);
            }

            return result;
        }
    }

    protected String getFilename(final UMOMessageAdapter adapter, String pattern, TemplateParser parser)
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                if (token.equals("DATE"))
                {
                    return DateUtils.getTimeStamp(DEFAULT_DATE_FORMAT);
                }
                else if (token.startsWith("DATE:"))
                {
                    token = token.substring(5);
                    return DateUtils.getTimeStamp(token);
                }
                else if (token.startsWith("UUID"))
                {
                    return UUID.getUUID();
                }
                else if (token.startsWith("SYSTIME"))
                {
                    return String.valueOf(System.currentTimeMillis());
                }
                else if (token.startsWith("COUNT"))
                {
                    return String.valueOf(count.getAndIncrement());
                }
                else if (adapter != null)
                {
                    if (token.startsWith("ORIGINALNAME"))
                    {
                        return adapter.getStringProperty(FileConnector.PROPERTY_FILENAME, null);
                    }
                    else
                    {
                        return adapter.getStringProperty(token, null);
                    }
                }
                return null;

            }
        }, pattern);
    }
}
