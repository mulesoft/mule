/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.resolver;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingUtils.locateParsingDelegate;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.object.CharsetValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.MediaTypeValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.resolver.BasicTypeValueResolverFactoryTypeVisitor;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * A {@link MetadataTypeVisitor} implementation that creates a {@link ValueResolver} instances depending on a parameter
 * {@link MetadataType}.
 *
 * @since 4.2
 */
public class ValueResolverFactoryTypeVisitor extends BasicTypeValueResolverFactoryTypeVisitor {

  private final List<ValueResolverParsingDelegate> valueResolverParsingDelegates =
      of(new CharsetValueResolverParsingDelegate(), new MediaTypeValueResolverParsingDelegate());
  private final ValueResolverParsingDelegate defaultValueResolverParsingDelegate = new DefaultValueResolverParsingDelegate();

  private final DslSyntaxResolver dslSyntaxResolver;
  private final Object defaultValue;
  private final boolean content;
  private final boolean acceptsReferences;

  public ValueResolverFactoryTypeVisitor(DslSyntaxResolver dslSyntaxResolver, String parameterName,
                                         Object value, Object defaultValue, boolean content, boolean acceptsReferences,
                                         Class<?> expectedClass) {
    super(parameterName, value, expectedClass);
    this.dslSyntaxResolver = dslSyntaxResolver;
    this.defaultValue = defaultValue;
    this.content = content;
    this.acceptsReferences = acceptsReferences;
  }

  @Override
  public void visitDateTime(DateTimeType dateTimeType) {
    setResolver(parseDate(getValue(), dateTimeType, defaultValue));
  }

  @Override
  public void visitDate(DateType dateType) {
    setResolver(parseDate(getValue(), dateType, defaultValue));
  }

  @Override
  public void visitObject(ObjectType objectType) {
    if (isMap(objectType)) {
      defaultVisit(objectType);
      return;
    }

    ValueResolver valueResolver;
    Optional<? extends ParsingDelegate> delegate = locateParsingDelegate(valueResolverParsingDelegates, objectType);
    Optional<DslElementSyntax> typeDsl = dslSyntaxResolver.resolve(objectType);

    if (delegate.isPresent() && typeDsl.isPresent()) {
      valueResolver = (ValueResolver) delegate.get().parse(getValue().toString(), objectType, typeDsl.get());
    } else {
      valueResolver = acceptsReferences
          ? defaultValueResolverParsingDelegate.parse(getValue().toString(), objectType, null)
          : new StaticValueResolver<>(getValue(), content);
    }

    setResolver(valueResolver);
  }

  @Override
  protected void defaultVisit(MetadataType metadataType) {
    ValueResolver delegateResolver = locateParsingDelegate(valueResolverParsingDelegates, metadataType)
        .map(delegate -> delegate.parse(getValue().toString(), metadataType, null))
        .orElseGet(() -> acceptsReferences
            ? defaultValueResolverParsingDelegate.parse(getValue().toString(), metadataType, null)
            : new TypeSafeValueResolverWrapper(new StaticValueResolver<>(getValue(), content), getExpectedClass()));

    setResolver(delegateResolver);
  }

  private ValueResolver parseDate(Object value, MetadataType dateType, Object defaultValue) {
    Class<?> type = getType(dateType);
    if (isExpression(value)) {
      return new TypeSafeExpressionValueResolver<>((String) value, type, toDataType(dateType));
    }

    if (value == null) {
      if (defaultValue == null) {
        return new StaticValueResolver<>(null, content);
      }

      value = defaultValue;
    }

    return doParseDate(value, type);
  }

  private ValueResolver doParseDate(Object value, Class<?> type) {

    if (value instanceof String) {
      Object constructedValue = null;
      DateTime dateTime = getParsedDateTime((String) value);

      if (type.equals(LocalDate.class)) {
        constructedValue = LocalDate.of(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
      } else if (type.equals(Date.class)) {
        constructedValue = dateTime.toDate();
      } else if (type.equals(LocalDateTime.class)) {
        Instant instant = ofEpochMilli(dateTime.getMillis());
        constructedValue = LocalDateTime.ofInstant(instant, ZoneId.of(dateTime.getZone().getID()));
      } else if (type.equals(Calendar.class)) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime.toDate());
        constructedValue = calendar;
      } else if (type.equals(ZonedDateTime.class)) {
        Instant instant = ofEpochMilli(dateTime.getMillis());
        constructedValue = ZonedDateTime.ofInstant(instant, ZoneId.of(dateTime.getZone().getID()));
      }

      if (constructedValue == null) {
        throw new IllegalArgumentException(format("Could not construct value of type '%s' from String '%s'", type.getName(),
                                                  value));
      } else {
        value = constructedValue;
      }
    }

    if (hasValidType(value)) {
      return new StaticValueResolver<>(value, content);
    }

    throw new IllegalArgumentException(format("Could not transform value of type '%s' to a valid date type",
                                              value != null ? value.getClass().getName() : "null"));
  }

  private DateTime getParsedDateTime(String value) {
    try {
      return ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(value);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(format("Could not parse value '%s' according to ISO 8601", value));
    }
  }

  private boolean hasValidType(Object value) {
    return value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime
        || value instanceof Calendar || value instanceof ZonedDateTime;
  }
}
