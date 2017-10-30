/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.removeDynamicAnnotations;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.namespace.QName;

@SmallTest
public class ComponentInvocationHandlerTestCase extends AbstractMuleTestCase {

  @Test
  public void notAnnotated() throws Exception {
    Component annotated = addAnnotationsToClass(NotAnnotated.class).newInstance();
    assertThat(annotated.getAnnotations().keySet(), empty());
    annotated.setAnnotations(singletonMap(LOCATION_KEY, "value"));
    assertThat(annotated.getAnnotations().keySet(), contains(LOCATION_KEY));

    assertThat(annotated.getClass().getMethod("setSomething", Object.class).isAnnotationPresent(Inject.class), is(true));

    assertThat(removeDynamicAnnotations(annotated), instanceOf(NotAnnotated.class));
    assertThat(removeDynamicAnnotations(annotated), not(instanceOf(Component.class)));
  }

  @Test
  public void extendsAbstractAnnotated() throws Exception {
    Component annotated = addAnnotationsToClass(ExtendsAnnotated.class).newInstance();
    assertThat(annotated.getAnnotations().keySet(), empty());
    annotated.setAnnotations(singletonMap(LOCATION_KEY, "value"));
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
    annotated.setAnnotations(singletonMap(LOCATION_KEY, "value"));
    assertThat(annotated.getAnnotations().keySet(), contains(LOCATION_KEY));
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

}
