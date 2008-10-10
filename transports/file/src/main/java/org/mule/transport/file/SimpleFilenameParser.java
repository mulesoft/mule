/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.transport.MessageAdapter;
import org.mule.util.DateUtils;
import org.mule.util.TemplateParser;
import org.mule.util.UUID;
import org.mule.util.expression.ExpressionEvaluatorManager;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SimpleFilenameParser</code> understands a limited set of tokens, namely
 * <ul>
 * <li>#[DATE] : the currrent date in the format dd-MM-yy_HH-mm-ss.SS</li>
 * <li>#[DATE:yy-MM-dd] : the current date using the specified format</li>
 * <li>#[SYSTIME] : The current system time milliseconds</li>
 * <li>#[UUID] : A generated Universally unique id</li>
 * <li>#[ORIGINALNAME] : The origial file name if the file being written was read
 * from another location</li>
 * <li>#[COUNT] : An incremental counter</li>
 * <li>#[&lt;Message Property Name>] : A name of a property on the message</li>
 * </ul>
 * Note that square brackets can be used instead of curl brackets, this is useful
 * when defining the file output pattern in a Mule Url endpointUri where the curl
 * bracket is an invalid character.
 * <p/>
 * Curly braces ${} are supported, but give a deprecation warning.
 * @deprecated Superceded by {@link org.mule.transport.file.ExpressionFilenameParser}, which is now
 * used by Mule implicitly.
 */
@Deprecated
public class SimpleFilenameParser implements FilenameParser
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy_HH-mm-ss.SSS";

    private final TemplateParser wigglyMuleParser = TemplateParser.createMuleStyleParser();
    private final TemplateParser antParser = TemplateParser.createAntStyleParser();
    private final TemplateParser squareParser = TemplateParser.createSquareBracesStyleParser();

    private final AtomicLong count = new AtomicLong(0);

    public String getFilename(MessageAdapter adapter, String pattern)
    {
        if (pattern == null)
        {
            return UUID.getUUID() + ".dat";
        }
        else
        {
            if (pattern.contains(ExpressionEvaluatorManager.DEFAULT_EXPRESSION_PREFIX))
            {
                return getFilename(adapter, pattern, wigglyMuleParser);
            }
            else if (pattern.contains("{"))
            {
                logger.warn("Found " + pattern + ". Using ${} style has been deprecated for Mule placeholders and won't be supported in the future," +
                            " please switch to #[] and ExpressionFilenameParser");
                return getFilename(adapter, pattern, antParser);
            }
            else
            {
                return getFilename(adapter, pattern, squareParser);
            }
        }
    }

    protected String getFilename(final MessageAdapter adapter, String pattern, TemplateParser parser)
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
                        return adapter.getStringProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, null);
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
