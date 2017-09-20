/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.ExpressionExecutor;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.el.datetime.DateTime;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionExecutor;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageContext;
import org.mule.mvel2.ParserConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DateTimeExpressionLanguageFunctionTestCase extends AbstractMuleTestCase {

  protected ExpressionExecutor<MVELExpressionLanguageContext> expressionExecutor;
  protected MVELExpressionLanguageContext context;
  protected DateTimeExpressionLanguageFuntion dateTimeFunction;

  @Before
  public void setup() throws InitialisationException {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    expressionExecutor = new MVELExpressionExecutor(parserConfiguration);
    context = new MVELExpressionLanguageContext(parserConfiguration, Mockito.mock(MuleContext.class));
    dateTimeFunction = new DateTimeExpressionLanguageFuntion();
    context.declareFunction("dateTime", dateTimeFunction);
  }

  @Test
  public void parseISO8601String() throws Exception {
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {"2013-03-17T00:23:00Z"}, context);
    assertNotNull(dateTime);
    assertEquals(2013, dateTime.getYear());
    assertEquals(3, dateTime.getMonth());
    assertEquals(17, dateTime.getDayOfMonth());
    assertEquals(0, dateTime.getHours());
    assertEquals(23, dateTime.getMinutes());
    assertEquals(0, dateTime.getSeconds());
    assertEquals(0, dateTime.toCalendar().get(Calendar.ZONE_OFFSET));
  }

  @Test
  public void parseISO8601StringWithTimeZome() throws Exception {
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {"2013-03-17T00:23:00+07:00"}, context);
    assertNotNull(dateTime);
    assertEquals(2013, dateTime.getYear());
    assertEquals(3, dateTime.getMonth());
    assertEquals(17, dateTime.getDayOfMonth());
    assertEquals(0, dateTime.getHours());
    assertEquals(23, dateTime.getMinutes());
    assertEquals(0, dateTime.getSeconds());
    assertEquals(7 * 1000 * 60 * 60, dateTime.toCalendar().get(Calendar.ZONE_OFFSET));
  }

  @Test
  public void parseFormattedString() throws Exception {
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {"17/3/13 00:23:00", "dd/M/yy hh:mm:ss"}, context);
    assertNotNull(dateTime);
    assertEquals(2013, dateTime.getYear());
    assertEquals(3, dateTime.getMonth());
    assertEquals(17, dateTime.getDayOfMonth());
    assertEquals(0, dateTime.getHours());
    assertEquals(23, dateTime.getMinutes());
    assertEquals(0, dateTime.getSeconds());
    assertEquals(0, dateTime.toCalendar().get(Calendar.ZONE_OFFSET));
  }

  @Test
  public void parseFormattedStringWithTimeZone() throws Exception {
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {"17/3/13 00:23:00 -0700", "dd/M/yy hh:mm:ss ZZ"}, context);
    assertNotNull(dateTime);
    assertEquals(2013, dateTime.getYear());
    assertEquals(3, dateTime.getMonth());
    assertEquals(17, dateTime.getDayOfMonth());
    assertEquals(0, dateTime.getHours());
    assertEquals(23, dateTime.getMinutes());
    assertEquals(0, dateTime.getSeconds());
    assertEquals(-7 * 1000 * 60 * 60, dateTime.toCalendar().get(Calendar.ZONE_OFFSET));
  }

  @Test
  public void convertDate() throws Exception {
    Date date = new Date();
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {date}, context);
    assertNotNull(dateTime);
    assertEquals(date.getYear() + 1900, dateTime.getYear());
    assertEquals(date.getMonth() + 1, dateTime.getMonth());
    assertEquals(date.getDate(), dateTime.getDayOfMonth());
    assertEquals(date.getHours(), dateTime.getHours());
    assertEquals(date.getMinutes(), dateTime.getMinutes());
    assertEquals(date.getSeconds(), dateTime.getSeconds());
  }

  @Test
  public void convertCalendar() throws Exception {
    Calendar cal = Calendar.getInstance();
    DateTime dateTime = (DateTime) dateTimeFunction.call(new Object[] {cal}, context);
    assertNotNull(dateTime);
    assertEquals(cal.get(Calendar.YEAR), dateTime.getYear());
    assertEquals(cal.get(Calendar.MONTH) + 1, dateTime.getMonth());
    assertEquals(cal.get(Calendar.DAY_OF_MONTH), dateTime.getDayOfMonth());
    assertEquals(cal.get(Calendar.HOUR_OF_DAY), dateTime.getHours());
    assertEquals(cal.get(Calendar.MINUTE), dateTime.getMinutes());
    assertEquals(cal.get(Calendar.SECOND), dateTime.getSeconds());
  }
}
