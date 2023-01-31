/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.junit.Test;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.semantics.security.Password;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeWrapperTestCase {

  @Test
  public void enumParameterType() {
    TypeWrapper type = new TypeWrapper(TestEnum.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    for (ExtensionParameter parameter : type.getMethod("compareTo", Enum.class).map(WithParameters::getParameters).get()) {
      assertThat(parameter.getType().getTypeName(), is(("java.lang.Enum")));
    }
  }

  @Test
  public void recursiveParameterType() {
    TypeWrapper type = new TypeWrapper(SomeClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    for (ExtensionParameter parameter : type.getMethod("someMethod", SomeClass.class).map(WithParameters::getParameters).get()) {
      assertThat(parameter.getType().getTypeName(),
                 is(("org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapperTestCase$SomeClass")));
    }
  }

  @Test
  public void getAnnotations() {
    Type type = new TypeWrapper(SomeClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    assertThat(type.getAnnotations().count(), is(2L));
    assertThat(type.getAnnotations().anyMatch(ann -> ann.getTypeName().equals("org.mule.sdk.api.annotation.Alias")), is(true));
    assertThat(type.getAnnotations()
        .anyMatch(ann -> ann.getTypeName().equals("org.mule.runtime.extension.api.annotation.Extension")),
               is(true));
  }

  @Test
  public void getAnnotationsForMethodWrapper() {
    java.util.Optional<MethodElement> method = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader())).getEnclosingMethods()
            .filter(m -> m.getName().contains("childMethod")).findAny();
    assertThat(method.isPresent(), is(true));
    assertThat(method.get().getAnnotations().count(), is(1L));
    assertThat(method.get().getAnnotations()
        .anyMatch(ann -> ann.getTypeName().equals("org.mule.runtime.extension.api.annotation.deprecated.Deprecated")),
               is(true));
  }

  @Test
  public void getAnnotationsForParameterWrapper() {
    java.util.Optional<MethodElement> method = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader())).getEnclosingMethods()
            .filter(m -> m.getName().contains("childMethod")).findAny();
    assertThat(method.isPresent(), is(true));
    assertThat(method.get().getParameters().size(), is(1));
    assertThat(method.get().getParameters().get(0).getAnnotations().count(), is(1L));
    assertThat(method.get().getParameters().get(0).getAnnotations()
        .anyMatch(ann -> ann.getTypeName().equals("org.mule.sdk.api.annotation.param.Optional")),
               is(true));
  }

  @Test
  public void getAnnotationsForFieldWrapper() {
    java.util.Optional<FieldElement> field = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader())).getFields().stream()
            .filter(f -> f.getName().contains("someField")).findAny();
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().getAnnotations().count(), is(1L));
    assertThat(field.get().getAnnotations()
        .anyMatch(ann -> ann.getTypeName().equals("org.mule.sdk.api.annotation.semantics.security.Password")),
               is(true));
  }

  @Test
  public void getEnclosingMethods() {
    TypeWrapper method = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    assertThat(method.getEnclosingMethods().anyMatch(m -> m.getName().contains("childMethod")), is(true));
    assertThat(method.getEnclosingMethods().anyMatch(m -> m.getName().contains("someMethod")), is(false));
  }

  @Test
  public void getImplementingInterfaces() {
    Type type = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    assertThat(type.getImplementingInterfaces().count(), is(1L));
    assertThat(type.getImplementingInterfaces()
        .anyMatch(i -> i.getTypeName()
            .contains("org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapperTestCase$TestInterface")),
               is(true));
  }

  @Test
  public void isArray() {
    java.util.Optional<FieldElement> field = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader())).getFields().stream()
            .filter(f -> f.getName().contains("arrayOfStrings")).findAny();
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().getType().isArray(), is(true));
    assertThat(field.get().getType().getArrayComponentType().get().getTypeName(), containsString("String"));
  }

  @Test
  public void listIsNotArray() {
    java.util.Optional<FieldElement> field = new TypeWrapper(ChildTestClass.class, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader())).getFields().stream()
            .filter(f -> f.getName().contains("listOfStrings")).findAny();
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().getType().isArray(), is(false));
    assertThat(field.get().getType().getArrayComponentType().isPresent(), is(false));
  }

  @Alias("Some Class Alias")
  @Extension(name = "Some Extension")
  // Similar to java.util.stream.BaseStream
  public class SomeClass<T extends SomeClass> {

    public void someMethod(T someParameter) {}
  }

  private interface TestInterface {
  }

  private class ChildTestClass extends SomeClass implements TestInterface {

    @Password
    String someField;

    @Deprecated(message = "No longer supported", since = "4.4")
    public void childMethod(@Optional String optionalParameter) {}

    String[] arrayOfStrings;

    List<Boolean> listOfStrings;
  }

  public enum TestEnum {
    SOME_VALUE, ANOTHER_VALUE
  }
}
