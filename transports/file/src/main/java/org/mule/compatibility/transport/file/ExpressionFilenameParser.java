/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.util.TemplateParser;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <code>ExpressionFilenameParser</code> can use any expression language supported by Mule to construct a file name for the
 * current message. Expressions can be xpath, xquery, mvel, header, function and more. For more information see
 * http://www.mulesoft.org/documentation/display/MULE3USER/Using+Expressions.
 * <p/>
 * For example an xpath expression can be defined to pull a message id out of an xml message and use that as the file name -
 * <code>
 * #[xpath:/message/header/@id]
 * </code>
 * <p/>
 * This parser superseeds the (now removed) <code>org.mule.compatibility.transport.file.SimpleFilenameParser</code> which has been
 * kept in Mule 2 for compatibility. The following demonstrates how to achieve the same results when using the
 * <code>ExpressionFilenameParser</code> over the <code>SimpleFilenameParser</code>
 * <ul>
 * <li>#[DATE] : #[org.mule.runtime.core.util.DateUtils.getTimeStamp('dd-MM-yy_HH-mm-ss.SSS')]</li>
 * <li>#[DATE:yy-MM-dd] : #[org.mule.runtime.core.util.DateUtils.getTimeStamp('yy-MM-dd')]</li>
 * <li>#[SYSTIME] : #[server.dateTime.toDate()]</li>
 * <li>#[UUID] : #[org.mule.runtime.core.util.UUID.getUUID()]</li>
 * <li>#[ORIGINALNAME] : #[message.inboundProperties.originalFilename]</li>
 * <li>#[COUNT] : #[org.mule.compatibility.transport.file.ExpressionFilenameParser.count()] - note that this is a global
 * counter.</li>
 * <li>#[&lt;Message Property Name&gt;] : #[message.outboundProperties&lt;Message Property Name&gt;]</li>
 * </ul>
 */
public class ExpressionFilenameParser implements FilenameParser, MuleContextAware {

  /**
   * A local counter that will increment for each call. If the server is re-started the counter will return to zero
   */
  private static final AtomicLong count = new AtomicLong(0);

  public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy_HH-mm-ss.SSS";
  public static final String DEFAULT_EXPRESSION =
      MessageFormat.format("{0}org.mule.runtime.core.util.UUID.getUUID(){1}.dat", ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                           ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

  private final TemplateParser wigglyMuleParser = TemplateParser.createMuleStyleParser();
  private final TemplateParser squareParser = TemplateParser.createSquareBracesStyleParser();

  protected MuleContext muleContext;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public String getFilename(MuleEvent event, String expression) {
    if (expression == null) {
      expression = DEFAULT_EXPRESSION;
    }

    if (expression.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX) > -1) {
      return getFilename(event, expression, wigglyMuleParser);
    } else {
      return getFilename(event, expression, squareParser);
    }
  }

  protected String getFilename(final MuleEvent event, String expression, TemplateParser parser) {
    return parser.parse(new TemplateParser.TemplateCallback() {

      @Override
      public Object match(String token) {
        return muleContext.getExpressionManager().evaluate(token, event);
      }
    }, expression);
  }

  public static Long count() {
    return count.getAndIncrement();
  }

  public static void resetCount() {
    count.set(0L);
  }
}
