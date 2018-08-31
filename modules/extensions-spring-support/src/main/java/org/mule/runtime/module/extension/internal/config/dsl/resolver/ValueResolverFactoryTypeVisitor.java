/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.resolver;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.object.*;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingUtils.locateParsingDelegate;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

/**
 * A {@link MetadataTypeVisitor} implementation that creates a {@link ValueResolver} instances
 * depending on a parameter {@link MetadataType}.
 *
 * @since 4.2
 */
public class ValueResolverFactoryTypeVisitor extends BasicTypeMetadataVisitor {

  private final ConversionService conversionService = new DefaultConversionService();
  private final List<ValueResolverParsingDelegate> valueResolverParsingDelegates =
      of(new CharsetValueResolverParsingDelegate(), new MediaTypeValueResolverParsingDelegate());
  private final ValueResolverParsingDelegate defaultValueResolverParsingDelegate = new DefaultValueResolverParsingDelegate();

  private final Reference<ValueResolver> resolverValueHolder = new Reference<>();
  private DslSyntaxResolver dslSyntaxResolver;
  private final String parameterName;
  private final MetadataType expected;
  private final Object value;
  private final Object defaultValue;
  private final boolean acceptsReferences;
  private final Class<?> expectedClass;


  public ValueResolverFactoryTypeVisitor(DslSyntaxResolver dslSyntaxResolver, String parameterName, MetadataType expected,
                                         Object value, Object defaultValue, boolean acceptsReferences, Class<?> expectedClass) {
    this.dslSyntaxResolver = dslSyntaxResolver;
    this.parameterName = parameterName;
    this.expected = expected;
    this.value = value;
    this.defaultValue = defaultValue;
    this.acceptsReferences = acceptsReferences;
    this.expectedClass = expectedClass;
  }

  public ValueResolver getResolver() {
    return resolverValueHolder.get();
  }

  @Override
  protected void visitBasicType(MetadataType metadataType) {
    if (conversionService.canConvert(value.getClass(), expectedClass)) {
      resolverValueHolder.set(new StaticValueResolver<>(convertSimpleValue(value, expectedClass, parameterName)));
    } else {
      defaultVisit(metadataType);
    }
  }

  @Override
  public void visitDateTime(DateTimeType dateTimeType) {
    resolverValueHolder.set(parseDate(value, dateTimeType, defaultValue));
  }

  @Override
  public void visitDate(DateType dateType) {
    resolverValueHolder.set(parseDate(value, dateType, defaultValue));
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
      valueResolver = (ValueResolver) delegate.get().parse(value.toString(), objectType, typeDsl.get());
    } else {
      valueResolver = acceptsReferences
          ? defaultValueResolverParsingDelegate.parse(value.toString(), objectType, null)
          : new StaticValueResolver<>(value);
    }

    resolverValueHolder.set(valueResolver);
  }

  @Override
  protected void defaultVisit(MetadataType metadataType) {
    ValueResolver delegateResolver = locateParsingDelegate(valueResolverParsingDelegates, metadataType)
        .map(delegate -> delegate.parse(value.toString(), metadataType, null))
        .orElseGet(() -> acceptsReferences
            ? defaultValueResolverParsingDelegate.parse(value.toString(), metadataType, null)
            : new TypeSafeValueResolverWrapper(new StaticValueResolver<>(value), expectedClass));

    resolverValueHolder.set(delegateResolver);
  }

  private ValueResolver parseDate(Object value, MetadataType dateType, Object defaultValue) {
    Class<?> type = getType(dateType);
    if (isExpression(value)) {
      return new TypeSafeExpressionValueResolver<>((String) value, type, toDataType(dateType));
    }

    if (value == null) {
      if (defaultValue == null) {
        return new StaticValueResolver<>(null);
      }

      value = defaultValue;
    }

    return doParseDate(value, type);
  }

  private Object convertSimpleValue(Object value, Class<?> expectedClass, String parameterName) {
    try {
      return conversionService.convert(value, expectedClass);
    } catch (Exception e) {
      throw new IllegalArgumentException(format("Could not transform simple value '%s' to type '%s' in parameter '%s'", value,
                                                expectedClass.getSimpleName(), parameterName));
    }
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
      }

      if (constructedValue == null) {
        throw new IllegalArgumentException(format("Could not construct value of type '%s' from String '%s'", type.getName(),
                                                  value));
      } else {
        value = constructedValue;
      }
    }

    if (value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Calendar) {
      return new StaticValueResolver<>(value);
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
}
