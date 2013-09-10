/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.util.TemplateParser;

import java.text.MessageFormat;

/**
 * <code>ExpressionFilenameParser</code> can use any expression language supported by Mule
 * to construct a file name for the current message.  Expressions can be xpath, xquery, ognl, mvel,
 * header, function and more. For more information see http://www.mulesoft.org/documentation/display/MULE3USER/Using+Expressions.
 * <p/>
 * For example an xpath expression can be defined to pull a message id out of an xml message and 
 * use that as the file name -
 * <code>
 * #[xpath:/message/header/@id]
 * </code>
 * <p/>
 * This parser superseeds the (now removed) <code>org.mule.transport.file.SimpleFilenameParser</code>
 * which has been kept in Mule 2 for compatibility. The following demonstrates how to achieve the 
 * same results when using the <code>ExpressionFilenameParser</code> over the 
 * <code>SimpleFilenameParser</code>
 * <ul>
 * <li>#[DATE] : #[function:datestamp]</li>
 * <li>#[DATE:yy-MM-dd] : #[function:datestamp-yy-MM-dd]</li>
 * <li>#[SYSTIME] : #[function:systime]</li>
 * <li>#[UUID] : #[function:uuid]</li>
 * <li>#[ORIGINALNAME] : #[header:inbound:originalFilename]</li>
 * <li>#[COUNT] : #[function:count] - note that this is a global counter.</li>
 * <li>#[&lt;Message Property Name&gt;] : #[header:&lt;Message Property Name&gt;]</li>
 * </ul>
 */
public class ExpressionFilenameParser implements FilenameParser, MuleContextAware
{
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy_HH-mm-ss.SSS";
    public static final String DEFAULT_EXPRESSION = MessageFormat.format("{0}function:uuid{1}.dat",
                                                                         ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                                         ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    private final TemplateParser wigglyMuleParser = TemplateParser.createMuleStyleParser();
    private final TemplateParser squareParser = TemplateParser.createSquareBracesStyleParser();

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getFilename(MuleMessage message, String expression)
    {
        if (expression == null)
        {
            expression = DEFAULT_EXPRESSION;
        }

        if (expression.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX) > -1)
        {
            return getFilename(message, expression, wigglyMuleParser);
        }
        else
        {
            return getFilename(message, expression, squareParser);
        }
    }

    protected String getFilename(final MuleMessage message, String expression, TemplateParser parser)
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                return muleContext.getExpressionManager().evaluate(token, message);
            }
        }, expression);
    }
}
