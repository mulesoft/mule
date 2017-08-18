/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.lang.System.lineSeparator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentOnlyLookupStrategy.PARENT_ONLY;
import static org.mule.tck.junit4.matcher.FunctionExpressionMatcher.expressionMatches;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.FineGrainedControlClassLoader;
import org.mule.runtime.module.artifact.api.classloader.exception.CompositeClassNotFoundException;
import org.mule.tck.classlaoder.TestClassLoader;
import org.mule.tck.classlaoder.TestClassLoader.TestClassNotFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class FineGrainedControlClassLoaderTestCase extends AbstractMuleTestCase {

  public static final String TEST_CLASS_PACKAGE = "mypackage";
  public static final String TEST_CLASS_NAME = TEST_CLASS_PACKAGE + ".MyClass";
  public static final String EXPECTED_CHILD_MESSAGE = "Bye";
  public static final String EXPECTED_PARENT_MESSAGE = "Hello";

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void usesParentOnlyLookup() throws Exception {
    URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_ONLY);
    FineGrainedControlClassLoader ext =
        new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

    assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
  }

  @Test
  public void usesParentOnlyLookupAndFails() throws Exception {
    ClassLoader parent = mock(ClassLoader.class);
    final ClassNotFoundException thrownException = new ClassNotFoundException("ERROR");
    when(parent.loadClass(TEST_CLASS_NAME)).thenThrow(thrownException);

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_ONLY);

    expected.expect(CompositeClassNotFoundException.class);
    expected.expectMessage(startsWith("Cannot load class '" + TEST_CLASS_NAME + "': [" + lineSeparator() + "\t" + "ERROR]"));
    expected.expect(expressionMatches((e) -> ((CompositeClassNotFoundException) e).getExceptions(),
                                      contains(sameInstance(thrownException))));

    FineGrainedControlClassLoader ext =
        new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

    ext.loadClass(TEST_CLASS_NAME);
  }

  @Test
  public void usesParentFirstLookup() throws Exception {
    URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

    FineGrainedControlClassLoader ext =
        new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

    assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
  }

  @Test
  public void usesParentFirstThenChildLookup() throws Exception {
    ClassLoader parent = Thread.currentThread().getContextClassLoader();

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);
    when(lookupPolicy.getClassLookupStrategy(Object.class.getName())).thenReturn(PARENT_FIRST);
    when(lookupPolicy.getClassLookupStrategy(String.class.getName())).thenReturn(PARENT_FIRST);

    FineGrainedControlClassLoader ext =
        new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

    assertEquals(EXPECTED_CHILD_MESSAGE, invokeTestClassMethod(ext));
  }

  @Test
  public void usesParentFirstAndChildLookupAndFails() throws Exception {
    ClassLoader parent = Thread.currentThread().getContextClassLoader();

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

    expected.expect(CompositeClassNotFoundException.class);
    expected.expectMessage(startsWith("Cannot load class '" + TEST_CLASS_NAME + "': ["));

    FineGrainedControlClassLoader ext = buildFineGrainedControlClassLoader(parent, lookupPolicy);

    expected.expect(expressionMatches((e) -> ((CompositeClassNotFoundException) e).getExceptions(),
                                      contains(hasMessage(is(TEST_CLASS_NAME)),
                                               expressionMatches((e) -> ((TestClassNotFoundException) e).getClassLoader(),
                                                                 is((ClassLoader) ext)))));

    invokeTestClassMethod(ext);
  }

  @Test
  public void usesChildFirstLookup() throws Exception {
    URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(CHILD_FIRST);
    when(lookupPolicy.getClassLookupStrategy(Object.class.getName())).thenReturn(PARENT_ONLY);
    when(lookupPolicy.getClassLookupStrategy(String.class.getName())).thenReturn(PARENT_ONLY);

    FineGrainedControlClassLoader ext =
        new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

    assertEquals(EXPECTED_CHILD_MESSAGE, invokeTestClassMethod(ext));
  }

  @Test
  public void usesChildFirstThenParentLookup() throws Exception {
    URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

    FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy);

    assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
  }

  @Test
  public void usesChildFirstThenParentLookupAndFails() throws Exception {
    ClassLoader parent = Thread.currentThread().getContextClassLoader();

    final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicy.getClassLookupStrategy(TEST_CLASS_NAME)).thenReturn(CHILD_FIRST);

    expected.expect(CompositeClassNotFoundException.class);
    expected.expectMessage(startsWith("Cannot load class '" + TEST_CLASS_NAME + "': ["));

    FineGrainedControlClassLoader ext = buildFineGrainedControlClassLoader(parent, lookupPolicy);

    expected.expect(expressionMatches((e) -> ((CompositeClassNotFoundException) e).getExceptions(),
                                      contains(expressionMatches((e) -> ((TestClassNotFoundException) e).getClassLoader(),
                                                                 is((ClassLoader) ext)),
                                               hasMessage(is(TEST_CLASS_NAME)))));

    invokeTestClassMethod(ext);
  }

  protected FineGrainedControlClassLoader buildFineGrainedControlClassLoader(ClassLoader parent,
                                                                             final ClassLoaderLookupPolicy lookupPolicy) {
    return new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy) {

      @Override
      public Class<?> findLocalClass(String name) throws ClassNotFoundException {
        try {
          return super.findLocalClass(name);
        } catch (ClassNotFoundException e) {
          throw new TestClassLoader.TestClassNotFoundException(name, this);
        }
      }
    };
  }

  private URL getParentResource() {
    return ClassUtils.getResource("classloader-test-hello.jar", this.getClass());
  }

  private URL getChildFileResource() {
    return ClassUtils.getResource("classloader-test-bye.jar", this.getClass());
  }

  private String invokeTestClassMethod(ClassLoader loader) throws Exception {
    Class cls = loader.loadClass(TEST_CLASS_NAME);
    Method method = cls.getMethod("hi");
    return (String) method.invoke(cls.newInstance());
  }
}
