/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Test;

public class LocalDateToDateTestCase extends AbstractMuleContextTestCase {

  private LocalDateToDate transformer;

  @Override
  protected void doSetUp() throws Exception {
    transformer = new LocalDateToDate();
    transformer.setMuleContext(muleContext);
  }

  @Test
  public void localDateToDate() throws Exception {
    LocalDate localDate = LocalDate.of(1983, 4, 20);
    Date date = (Date) transformer.transform(localDate);

    assertEquals(localDate.atTime(0, 0), date);
  }

  @Test
  public void localDateTimeToDate() throws Exception {
    LocalDateTime localDateTime = LocalDateTime.of(1983, 4, 20, 21, 15);
    Date date = (Date) transformer.transform(localDateTime);

    assertEquals(localDateTime, date);
  }

  private void assertEquals(LocalDateTime dateTime, Date date) {
    assertThat(dateTime.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli(), equalTo(date.getTime()));
  }
}
