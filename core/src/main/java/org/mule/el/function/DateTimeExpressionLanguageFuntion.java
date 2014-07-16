/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.function;

import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.el.datetime.DateTime;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

public class DateTimeExpressionLanguageFuntion implements ExpressionLanguageFunction
{

    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {
        int numParams = params.length;
        if (numParams < 1 || numParams > 3)
        {
            throw new IllegalArgumentException("invalid number of arguments");
        }

        if (numParams == 1)
        {
            Object dateParam = params[0];
            if (dateParam instanceof Date)
            {
                return new DateTime((Date) dateParam);
            }
            else if (dateParam instanceof Calendar)
            {
                return new DateTime((Calendar) dateParam);
            }
            else if (dateParam instanceof XMLGregorianCalendar)
            {
                return new DateTime((XMLGregorianCalendar) dateParam);
            }
            else
            {
                try
                {
                    return new DateTime(dateParam.toString());
                }
                catch (RuntimeException e)
                {
                    throw new ExpressionRuntimeException(
                        CoreMessages.createStaticMessage("Unable to parse string as a ISO-8601 date"), e);
                }
            }
        }
        else
        {
            try
            {
                return new DateTime((String) params[0], (String) params[1]);
            }
            catch (ParseException e)
            {
                throw new ExpressionRuntimeException(
                    CoreMessages.createStaticMessage("Unable to parse string as a date using format '"
                                                     + params[1] + "''"));
            }
        }
    }
}
