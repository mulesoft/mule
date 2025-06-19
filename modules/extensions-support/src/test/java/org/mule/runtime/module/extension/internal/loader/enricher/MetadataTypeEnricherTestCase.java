/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultAttributeFieldType;
import org.mule.metadata.api.model.impl.DefaultAttributeKeyType;
import org.mule.metadata.api.model.impl.DefaultBinaryType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultDateTimeType;
import org.mule.metadata.api.model.impl.DefaultDateType;
import org.mule.metadata.api.model.impl.DefaultFunctionType;
import org.mule.metadata.api.model.impl.DefaultIntersectionType;
import org.mule.metadata.api.model.impl.DefaultLocalDateTimeType;
import org.mule.metadata.api.model.impl.DefaultLocalTimeType;
import org.mule.metadata.api.model.impl.DefaultNothingType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectFieldType;
import org.mule.metadata.api.model.impl.DefaultObjectKeyType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultPeriodType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.api.model.impl.DefaultTimeType;
import org.mule.metadata.api.model.impl.DefaultTimeZoneType;
import org.mule.metadata.api.model.impl.DefaultTupleType;
import org.mule.metadata.api.model.impl.DefaultTypeParameterType;
import org.mule.metadata.api.model.impl.DefaultUnionType;
import org.mule.runtime.module.extension.internal.loader.enricher.MetadataTypeEnricher;

import java.util.Set;

import javax.xml.namespace.QName;

import org.junit.Test;

public class MetadataTypeEnricherTestCase {

  MetadataTypeEnricher enricher = new MetadataTypeEnricher();
  Set<TypeAnnotation> annotations = singleton(new TypeAliasAnnotation("alias"));

  @Test
  public void enrichAnyType() {
    verify(new DefaultAnyType(JAVA, emptyMap()));
  }

  @Test
  public void enrichArrayType() {
    verify(new DefaultArrayType(() -> new DefaultAnyType(JAVA, emptyMap()), JAVA, emptyMap()));
  }

  @Test
  public void enrichBinaryType() {
    verify(new DefaultBinaryType(JAVA, emptyMap()));
  }

  @Test
  public void enrichBooleanType() {
    verify(new DefaultBooleanType(JAVA, emptyMap()));
  }

  @Test
  public void enrichDateTimeType() {
    verify(new DefaultDateTimeType(JAVA, emptyMap()));
  }

  @Test
  public void enrichDateType() {
    verify(new DefaultDateType(JAVA, emptyMap()));
  }

  @Test
  public void enrichNumberType() {
    verify(new DefaultNumberType(JAVA, emptyMap()));
  }

  @Test
  public void enrichObjectType() {
    verify(new DefaultObjectType(emptyList(), false, null, JAVA, emptyMap()));
  }

  @Test
  public void enrichStringType() {
    verify(new DefaultStringType(JAVA, emptyMap()));
  }

  @Test
  public void enrichTimeType() {
    verify(new DefaultTimeType(JAVA, emptyMap()));
  }

  @Test
  public void enrichTupleType() {
    verify(new DefaultTupleType(singletonList(new DefaultAnyType(JAVA, emptyMap())), JAVA, emptyMap()));
  }

  @Test
  public void enrichUnionType() {
    verify(new DefaultUnionType(singletonList(new DefaultAnyType(JAVA, emptyMap())), JAVA, emptyMap()));
  }

  @Test
  public void enrichObjectKeyType() {
    verify(new DefaultObjectKeyType(of(new QName("Name")), of(compile("")), emptyList(), JAVA, emptyMap()));
  }

  @Test
  public void enrichAttributeKeyType() {
    verify(new DefaultAttributeKeyType(of(new QName("Name")), of(compile("")), JAVA, emptyMap()));
  }

  @Test
  public void enrichAttributeFieldType() {
    verify(new DefaultAttributeFieldType(new DefaultAttributeKeyType(of(new QName("Name")), of(compile("")), JAVA, emptyMap()),
                                         new DefaultAnyType(JAVA, emptyMap()), false, JAVA, emptyMap()));
  }

  @Test
  public void enrichObjectFieldType() {
    verify(new DefaultObjectFieldType(new DefaultObjectKeyType(of(new QName("Name")), of(compile("")), emptyList(), JAVA,
                                                               emptyMap()),
                                      new DefaultAnyType(JAVA, emptyMap()), false, false, JAVA, emptyMap()));
  }

  @Test
  public void enrichNothingType() {
    verify(new DefaultNothingType(JAVA, emptyMap()));
  }

  @Test
  public void enrichFunctionType() {
    verify(new DefaultFunctionType(JAVA, emptyMap(), empty(), emptyList()));
  }

  @Test
  public void enrichLocalDateTimeType() {
    verify(new DefaultLocalDateTimeType(JAVA, emptyMap()));
  }

  @Test
  public void enrichLocalTimeType() {
    verify(new DefaultLocalTimeType(JAVA, emptyMap()));
  }

  @Test
  public void enrichPeriodType() {
    verify(new DefaultPeriodType(JAVA, emptyMap()));
  }

  @Test
  public void enrichTimeZoneType() {
    verify(new DefaultTimeZoneType(JAVA, emptyMap()));
  }

  @Test
  public void enrichTypeParameterType() {
    verify(new DefaultTypeParameterType("Type Name", JAVA, emptyMap()));
  }

  @Test
  public void enrichIntersectionType() {
    verify(new DefaultIntersectionType(singletonList(new DefaultAnyType(JAVA, emptyMap())), JAVA, emptyMap()));
  }

  public void verify(MetadataType type) {
    MetadataType enrichedType = enricher.enrich(type, annotations);
    assertThat(enrichedType.getAnnotation(TypeAliasAnnotation.class).isPresent(), is(true));
    assertThat(enrichedType.getAnnotation(TypeAliasAnnotation.class).get().getValue(), is("alias"));
  }

}
