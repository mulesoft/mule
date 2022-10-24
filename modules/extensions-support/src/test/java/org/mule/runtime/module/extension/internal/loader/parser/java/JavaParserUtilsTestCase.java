/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.sdk.api.annotation.semantics.security.Username;
import org.mule.sdk.api.client.ExtensionsClient;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.tx.SourceTransactionalAction;
import org.mule.sdk.api.tx.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils.calculateFromClass;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

public class JavaParserUtilsTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void javaLangClassReturns410() {
    Type clazz = new TypeWrapper(String.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.1.0"));
  }

  @Test
  public void javaPrimitiveReturns410() {
    Type clazz = new TypeWrapper(long.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.1.0"));
  }

  @Test
  public void getFromRandomClass() {
    Type clazz = new TypeWrapper(CoreMatchers.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.1.0"));
  }

  @Test
  public void getFromClass() {
    Type clazz = new TypeWrapper(Source.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4"));
  }

  @Test
  public void getFromSuperClass() {
    Type clazz = new TypeWrapper(TestSource.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4"));
  }

  @Test
  public void getFromSuperSuperClass() {
    Type clazz = new TypeWrapper(SecondTestSource.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4"));
  }

  @Test
  public void getFromImplementedInterface() {
    Type clazz = new TypeWrapper(ImplementsInterface.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4.0"));
  }

  @Test
  public void getFromAnnotation() {
    Type clazz = new TypeWrapper(OAuthParameter.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.5.0"));
  }

  @Test
  public void recursiveClass() {
    Type clazz = new TypeWrapper(RecursiveClass.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.5.0"));
  }

  @Test
  public void getFromSuperClassGenerics() {
    Type clazz = new TypeWrapper(GenericsSource.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.5.0"));
  }

  @Test
  public void getFromFieldThrowsException() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage("Min Mule Version annotation is not allowed at the field level. Offending field: annotatedField in Class: 'WithAnnotatedField'");
    calculateFromClass(new TypeWrapper(WithAnnotatedField.class, TYPE_LOADER));
  }

  @Test
  public void getFromFieldAllowedInEnums() {
    Type clazz = new TypeWrapper(SourceTransactionalAction.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.5.0"));
  }

  @Test
  public void getFromSdkApiField() {
    Type clazz = new TypeWrapper(WithSdkApiField.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4"));
  }

  @Test
  public void getFromAnnotatedMethod() {
    Type clazz = new TypeWrapper(WithAnnotatedMethod.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.3.1"));
  }

  @Test
  public void getFromSdkApiArgument() {
    Type clazz = new TypeWrapper(WithWithSdkApiArgument.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.5.0"));
  }

  @Test
  public void nonEnforcedSdkApiClass() {
    Type clazz = new TypeWrapper(WithNonEnforcedMinMuleVersion.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.1.0"));
  }

  @Test
  public void atClassLevelAnnotationTakesPrecedence() {
    Type clazz = new TypeWrapper(AnnotatedAtClassLevel.class, TYPE_LOADER);
    assertThat(calculateFromClass(clazz), is("4.4.0"));
  }

  private class TestSource extends Source<String, Void> {

    @Override
    public void onStart(SourceCallback<String, Void> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  private class SecondTestSource extends TestSource {

    @Override
    public void onStart(SourceCallback<String, Void> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  @MinMuleVersion("4.5.0")
  private class RecursiveClass {

    RecursiveClass recursiveField;
  }

  private class GenericsSource extends Source<RecursiveClass, Void> {

    @Override
    public void onStart(SourceCallback<RecursiveClass, Void> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  private class ImplementsInterface implements Transactional {

    @Override
    public void begin() throws TransactionException {}

    @Override
    public void commit() throws TransactionException {}

    @Override
    public void rollback() throws TransactionException {}
  }

  private class WithAnnotatedField {

    @MinMuleVersion("4.4.0")
    String annotatedField;
  }

  private class WithSdkApiField {

    Result<String, String> sdkApiField;
  }

  private class WithAnnotatedMethod {

    @MinMuleVersion("4.3.1")
    public void annotatedMethod(String someArgument) {}
  }

  private class WithWithSdkApiArgument {

    public void annotatedMethod(ExtensionsClient extensionsClient) {}
  }

  private class WithNonEnforcedMinMuleVersion {

    @Username
    String user;
  }

  @MinMuleVersion("4.4.0")
  private class AnnotatedAtClassLevel {

    @Alias("alias")
    String name;

    @MinMuleVersion("4.6.0")
    public void someMethod(ExtensionsClient extensionsClient) {}
  }

  private enum AnnotatedEnum {
    NON_ANNOTATED_VALUE, @MinMuleVersion("4.6.0")
    ANNOTATED_VALUE
  }
}
