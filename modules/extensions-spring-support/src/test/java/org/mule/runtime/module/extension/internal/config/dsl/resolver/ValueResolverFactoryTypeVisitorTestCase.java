/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.DateTimeBuilder;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactoryTypeVisitor;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class ValueResolverFactoryTypeVisitorTestCase {

  private static final LocalDate LOCAL_DATE = LocalDate.parse("2021-04-28");
  private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.parse("2021-04-28T14:30:35");
  private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.parse("2021-04-27T15:30:00Z");

  private DslSyntaxResolver dslSyntaxResolver;
  private ValueResolvingContext valueResolvingContext;

  @Before
  public void setup() {
    dslSyntaxResolver = mock(DslSyntaxResolver.class);
    valueResolvingContext = mock(ValueResolvingContext.class);
  }

  @Test
  public void localDateValueIsResolved() throws MuleException {
    Class theClass = LocalDate.class;
    ValueResolverFactoryTypeVisitor visitor = createValueResolverFactoryTypeVisitor("2021-04-28", theClass);
    visitor.visitDateTime(createDateTimeType(theClass));
    LocalDate localDate = getVisitedValue(visitor);
    boolean isEqual = LOCAL_DATE.isEqual(localDate);
    assertThat(isEqual, is(true));
  }

  @Test
  public void dateValueIsResolved() throws Exception {
    Class theClass = Date.class;
    ValueResolverFactoryTypeVisitor visitor = createValueResolverFactoryTypeVisitor("2021-04-28", theClass);
    visitor.visitDateTime(createDateTimeType(theClass));
    Date date = getVisitedValue(visitor);
    boolean isEqual = new SimpleDateFormat("yyyy-MM-dd").parse("2021-04-28").equals(date);
    assertThat(isEqual, is(true));
  }

  @Test
  public void localDateTimeValueIsResolved() throws MuleException {
    Class theClass = LocalDateTime.class;
    ValueResolverFactoryTypeVisitor visitor = createValueResolverFactoryTypeVisitor("2021-04-28T14:30:35", theClass);
    visitor.visitDateTime(createDateTimeType(theClass));
    LocalDateTime localDateTime = getVisitedValue(visitor);
    boolean isEqual = LOCAL_DATE_TIME.isEqual(localDateTime);
    assertThat(isEqual, is(true));
  }

  @Test
  public void zonedDateTimeValueIsResolved() throws MuleException {
    Class theClass = ZonedDateTime.class;
    ValueResolverFactoryTypeVisitor visitor = createValueResolverFactoryTypeVisitor("2021-04-27T15:30:00Z", theClass);
    visitor.visitDateTime(createDateTimeType(theClass));
    ZonedDateTime zonedDateTime = getVisitedValue(visitor);
    boolean isEqual = ZONED_DATE_TIME.isEqual(zonedDateTime);
    assertThat(isEqual, is(true));
  }

  private ValueResolverFactoryTypeVisitor createValueResolverFactoryTypeVisitor(Object value, Class<?> expectedClass) {
    return new ValueResolverFactoryTypeVisitor("parameter",
                                               value, null, false, expectedClass);
  }

  private DateTimeType createDateTimeType(Class<?> expectedClass) {
    DateTimeBuilder builder = new BaseTypeBuilder(MetadataFormat.JAVA).dateTimeType().id(expectedClass.getCanonicalName());
    return builder.build();
  }

  private <T> T getVisitedValue(ValueResolverFactoryTypeVisitor visitor) throws MuleException {
    return (T) visitor.getResolver().resolve(valueResolvingContext);
  }
}
