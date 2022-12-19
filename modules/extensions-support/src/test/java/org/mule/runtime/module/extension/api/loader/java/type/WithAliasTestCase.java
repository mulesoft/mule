/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ClassBasedAnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.sdk.api.annotation.Alias;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WithAliasTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void getAliasFromClassAnnotatedWithSdkAliasAnnotation() {
    WithSdkAliasImpl withAlias = new WithSdkAliasImpl();
    assertThat(withAlias.getAlias(), is("SdkAliasAnnotation"));
  }

  @Test
  public void getAliasFromClassAnnotatedWithLegacyAliasAnnotation() {
    WithLegacyAliasImpl withAlias = new WithLegacyAliasImpl();
    assertThat(withAlias.getAlias(), is("legacyAliasAnnotation"));
  }

  @Test
  public void getAliasAnnotationFromClassWithLegacyAndSdkAnnotationShouldThrowError() {
    expectedException.expect(instanceOf(IllegalModelDefinitionException.class));
    expectedException.expectMessage("Both org.mule.runtime.extension.api.annotation.Alias and " +
        "org.mule.sdk.api.annotation.Alias annotations are present on element 'elementTest'");

    WithBothAliasImpl withAlias = new WithBothAliasImpl();
    withAlias.getAlias();
  }

  @Alias("SdkAliasAnnotation")
  private static class WithSdkAliasImpl implements WithAlias {

    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
      return ofNullable(getClass().getAnnotation(annotationClass));
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationClass) {
      return getClass().getAnnotation(annotationClass) != null;
    }

    @Override
    public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
      return isAnnotatedWith(annotationClass)
          ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, WithSdkAliasImpl.class, TYPE_LOADER))
          : empty();
    }

    @Override
    public Stream<Type> getAnnotations() {
      return stream(getClass().getAnnotations()).map(ann -> new TypeWrapper(ann.annotationType(), TYPE_LOADER));
    }

    @Override
    public String getName() {
      return "elementTest";
    }
  }

  @org.mule.runtime.extension.api.annotation.Alias("legacyAliasAnnotation")
  private static class WithLegacyAliasImpl implements WithAlias {

    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
      return ofNullable(getClass().getAnnotation(annotationClass));
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationClass) {
      return getClass().getAnnotation(annotationClass) != null;
    }

    @Override
    public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
      return isAnnotatedWith(annotationClass)
          ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, WithLegacyAliasImpl.class, TYPE_LOADER))
          : empty();
    }

    @Override
    public Stream<Type> getAnnotations() {
      return stream(getClass().getAnnotations()).map(ann -> new TypeWrapper(ann.annotationType(), TYPE_LOADER));
    }

    @Override
    public String getName() {
      return "elementTest";
    }
  }

  @Alias("SdkAliasAnnotation")
  @org.mule.runtime.extension.api.annotation.Alias("legacyAliasAnnotation")
  private static class WithBothAliasImpl implements WithAlias {

    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
      return ofNullable(getClass().getAnnotation(annotationClass));
    }

    @Override
    public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
      return isAnnotatedWith(annotationClass)
          ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, WithBothAliasImpl.class, TYPE_LOADER))
          : empty();
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationClass) {
      return getClass().getAnnotation(annotationClass) != null;
    }

    @Override
    public Stream<Type> getAnnotations() {
      return stream(getClass().getAnnotations()).map(ann -> new TypeWrapper(ann.annotationType(), TYPE_LOADER));
    }

    @Override
    public String getName() {
      return "elementTest";
    }
  }

}
