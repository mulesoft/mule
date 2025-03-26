/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.component;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.module.extension.internal.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.module.extension.internal.component.AnnotatedObjectInvocationHandler.removeDynamicAnnotations;

import static java.util.Collections.singletonMap;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.inject.Inject;

import javax.xml.namespace.QName;

import org.junit.Test;

import io.qameta.allure.Issue;

import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;

@SmallTest
public class ComponentInvocationHandlerTestCase extends AbstractMuleTestCase {

  @Test
  public void notAnnotated() throws Exception {
    Component annotated = addAnnotationsToClass(NotAnnotated.class).newInstance();
    assertThat(annotated.getAnnotations().keySet(), empty());
    annotated.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("value")));
    assertThat(annotated.getAnnotations().keySet(), contains(LOCATION_KEY));

    assertThat(annotated.getClass().getMethod("setSomething", Object.class).isAnnotationPresent(Inject.class), is(true));

    assertThat(removeDynamicAnnotations(annotated), instanceOf(NotAnnotated.class));
    assertThat(removeDynamicAnnotations(annotated), not(instanceOf(Component.class)));
  }

  @Test
  public void notAnnotatedWithConstructor() throws Exception {
    NotAnnotatedWithConstructor annotated =
        (NotAnnotatedWithConstructor) addAnnotationsToClass(NotAnnotatedWithConstructor.class).newInstance();

    assertThat(annotated.getSomething(), equalTo("something"));
  }

  @Test
  public void extendsAbstractAnnotated() throws Exception {
    Component annotated = addAnnotationsToClass(ExtendsAnnotated.class).newInstance();
    assertThat(annotated.getAnnotations().keySet(), empty());
    annotated.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("value")));
    assertThat(annotated.getAnnotations().keySet(), contains(LOCATION_KEY));

    assertThat(annotated.getClass().getMethod("setSomething", Object.class).isAnnotationPresent(Inject.class), is(true));
  }

  @Test
  public void overridesAbstractAnnotated() throws Exception {
    Class<Component> clazz = addAnnotationsToClass(ExtendsAnnotated.class);

    Method method = clazz.getMethod("setSomething", Object.class);

    assertThat(method.getAnnotation(Inject.class), not(nullValue()));
  }

  @Test
  public void implementsAnnotated() throws Exception {
    Component annotated = addAnnotationsToClass(ImplementsAnnotated.class).newInstance();
    assertThat(annotated.getAnnotations(), is(nullValue()));
  }

  @Test
  public void implementsInitialisable() throws Exception {
    Component annotated = addAnnotationsToClass(ImplementsInitialisable.class).newInstance();
    assertThat(annotated.getAnnotations().keySet(), empty());
    annotated.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("value")));
    assertThat(annotated.getAnnotations().keySet(), contains(LOCATION_KEY));
  }

  @Test
  public void serializable() throws Exception {
    Component annotated = addAnnotationsToClass(IsSerializable.class).newInstance();
    annotated.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("value")));
    ((IsSerializable) annotated).setValue("Hello World!");

    final ByteArrayOutputStream serializedFormStream = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(serializedFormStream);

    oos.writeObject(annotated);

    final Object deserialized = new ObjectInputStream(new ByteArrayInputStream(serializedFormStream.toByteArray())).readObject();

    assertThat(deserialized, instanceOf(IsSerializable.class));
    assertThat(deserialized, not(instanceOf(Component.class)));
    assertThat(((IsSerializable) deserialized).getValue(), is("Hello World!"));
  }

  @Test
  public void differentClassLoader() throws Exception {
    ClassLoader childCl = createDelegatorClassLoader();

    Class<Component> annotatedClass = addAnnotationsToClass(childCl.loadClass(Delegator.class.getName()));
    // ByteBuddy creates the class in an own class loader
    assertThat(annotatedClass.getClassLoader(), instanceOf(MultipleParentClassLoader.class));
  }

  @Test
  @Issue("W-10781796")
  public void whenClassDoesntDeclareToStringThenTheObjectToStringIsDecoratedWithLocation() throws Exception {
    Component annotated = addAnnotationsToClass(NotOverridingToString.class).newInstance();
    assertThat(annotated.toString(), containsString("location"));
  }

  @Test
  @Issue("W-10781796")
  public void whenClassDoesDeclareToStringThenTheOverriddenToStringIsUsed() throws Exception {
    Component annotated = addAnnotationsToClass(OverridingToString.class).newInstance();
    assertThat(annotated.toString(), is("Expected string"));
  }

  @Test
  @Issue("W-10781796")
  public void whenClassExtendsAnotherThatDeclaresToStringThenTheOverriddenToStringIsUsed() throws Exception {
    Component annotated = addAnnotationsToClass(ExtendingClassThatOverridesToString.class).newInstance();
    assertThat(annotated.toString(), is("Expected string"));
  }

  @Test
  public void fromJdk() throws Exception {
    Object notAnnotated = addAnnotationsToClass(Object.class).newInstance();

    assertThat(notAnnotated, not(instanceOf(Component.class)));
  }

  private ClassLoader createDelegatorClassLoader() {
    ClassLoader testClassLoader = new ClassLoader(this.getClass().getClassLoader()) {

      private Class<?> delegatorClassDefined = null;

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (Delegator.class.getName().equals(name)) {
          if (delegatorClassDefined == null) {
            byte[] classBytes;
            try {
              classBytes =
                  toByteArray(this.getClass()
                      .getResourceAsStream("/org/mule/runtime/module/extension/internal/component/Delegator.class"));
              delegatorClassDefined = this.defineClass(null, classBytes, 0, classBytes.length);
            } catch (Exception e) {
              return super.loadClass(name);
            }
          }

          return delegatorClassDefined;
        } else {
          return super.loadClass(name);
        }
      }
    };
    return testClassLoader;
  }

  public static class NotAnnotated {

    private Object something;

    @Inject
    public void setSomething(Object something) {
      this.something = something;
    }

    public Object getSomething() {
      return something;
    }

  }

  public static class NotAnnotatedWithConstructor {

    private final Object something;

    public NotAnnotatedWithConstructor() {
      this.something = "something";
    }

    public Object getSomething() {
      return something;
    }

  }

  public static class ExtendsAnnotated extends AbstractComponent {

    @Inject
    public void setSomething(Object something) {
      // Nothing to do
    }

  }

  public static class ImplementsAnnotated implements Component {

    @Override
    public Object getAnnotation(QName name) {
      return null;
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return null;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {}

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public Location getRootContainerLocation() {
      return null;
    }
  }

  public static class ImplementsInitialisable implements Initialisable {

    @Override
    public void initialise() throws InitialisationException {}

  }

  public static class IsSerializable implements Serializable {

    private static final long serialVersionUID = 9207437524543640942L;

    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

  }

  public static class OverridingToString {

    @Override
    public String toString() {
      return "Expected string";
    }
  }

  public static class ExtendingClassThatOverridesToString extends OverridingToString {

    // Doesnt override toString, but extends a class which does it.
  }

  public static class NotOverridingToString {

    // Doesnt override toString.
  }

}
