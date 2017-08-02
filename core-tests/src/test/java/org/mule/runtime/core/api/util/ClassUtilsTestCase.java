/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.AbstractFruit;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.Orange;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

@SmallTest
public class ClassUtilsTestCase extends AbstractMuleTestCase {

  // we do not want to match these methods when looking for a service method to
  // invoke
  protected final Set<String> ignoreMethods = new HashSet<String>(asList("equals", "getInvocationHandler"));

  @Test
  public void testIsConcrete() throws Exception {
    assertTrue(ClassUtils.isConcrete(Orange.class));
    assertTrue(!ClassUtils.isConcrete(Fruit.class));
    assertTrue(!ClassUtils.isConcrete(AbstractFruit.class));

    try {
      ClassUtils.isConcrete(null);
      fail("Class cannot be null, exception should be thrown");
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void testLoadClass() throws Exception {
    Class clazz = ClassUtils.loadClass("java.lang.String", getClass());
    assertNotNull(clazz);

    assertEquals(clazz.getName(), "java.lang.String");

    try {
      ClassUtils.loadClass("java.lang.Bing", getClass());
      fail("ClassNotFoundException should be thrown");
    } catch (ClassNotFoundException e) {
      // expected
    }

  }

  @Test
  public void testLoadPrimitiveClass() throws Exception {
    assertSame(ClassUtils.loadClass("boolean", getClass()), Boolean.TYPE);
    assertSame(ClassUtils.loadClass("byte", getClass()), Byte.TYPE);
    assertSame(ClassUtils.loadClass("char", getClass()), Character.TYPE);
    assertSame(ClassUtils.loadClass("double", getClass()), Double.TYPE);
    assertSame(ClassUtils.loadClass("float", getClass()), Float.TYPE);
    assertSame(ClassUtils.loadClass("int", getClass()), Integer.TYPE);
    assertSame(ClassUtils.loadClass("long", getClass()), Long.TYPE);
    assertSame(ClassUtils.loadClass("short", getClass()), Short.TYPE);
  }

  @Test
  public void testLoadClassOfType() throws Exception {

    Class<? extends Exception> clazz = ClassUtils.loadClass("java.lang.IllegalArgumentException", getClass(), Exception.class);
    assertNotNull(clazz);

    assertEquals(clazz.getName(), "java.lang.IllegalArgumentException");

    try {
      ClassUtils.loadClass("java.lang.UnsupportedOperationException", getClass(), String.class);
      fail("IllegalArgumentException should be thrown since class is not of expected type");
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  @Test
  public void testInstanciateClass() throws Exception {
    Object object = ClassUtils.instantiateClass("org.mule.tck.testmodels.fruit.Orange");
    assertNotNull(object);
    assertTrue(object instanceof Orange);

    object = ClassUtils.instantiateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Apple(), new Banana());
    assertNotNull(object);
    assertTrue(object instanceof FruitBowl);

    FruitBowl bowl = (FruitBowl) object;

    assertTrue(bowl.hasApple());
    assertTrue(bowl.hasBanana());

    try {
      ClassUtils.instantiateClass("java.lang.Bing");
      fail("Class does not exist, ClassNotFoundException should have been thrown");
    } catch (ClassNotFoundException e) {
      // expected
    }

  }

  @Test
  public void testGetParameterTypes() throws Exception {
    FruitBowl bowl = new FruitBowl();

    Class[] classes = ClassUtils.getParameterTypes(bowl, "apple");
    assertNotNull(classes);
    assertEquals(1, classes.length);
    assertEquals(Apple.class, classes[0]);

    classes = ClassUtils.getParameterTypes(bowl, "invalid");
    assertNotNull(classes);
    assertEquals(0, classes.length);
  }

  @Test
  public void testLoadingResources() throws Exception {
    URL resource = ClassUtils.getResource("log4j2-test.xml", getClass());
    assertNotNull(resource);

    resource = ClassUtils.getResource("does-not-exist.properties", getClass());
    assertNull(resource);
  }

  @Test
  public void testLoadingResourceEnumeration() throws Exception {
    Enumeration enumeration = ClassUtils.getResources("log4j2-test.xml", getClass());
    assertNotNull(enumeration);
    assertTrue(enumeration.hasMoreElements());

    enumeration = ClassUtils.getResources("does-not-exist.properties", getClass());
    assertNotNull(enumeration);
    assertTrue(!enumeration.hasMoreElements());
  }

  @Test
  public void testSimpleName() {
    simpleNameHelper("String", "foo".getClass());
    simpleNameHelper("int[]", (new int[0]).getClass());
    simpleNameHelper("Object[][]", (new Object[0][0]).getClass());
    simpleNameHelper("null", null);
  }

  @Test
  public void testEqual() {
    Object a1 = new HashBlob(1);
    Object a2 = new HashBlob(1);
    Object b = new HashBlob(2);
    assertTrue(ClassUtils.equal(a1, a2));
    assertTrue(ClassUtils.equal(b, b));
    assertTrue(ClassUtils.equal(null, null));
    assertFalse(ClassUtils.equal(a1, b));
    assertFalse(ClassUtils.equal(a2, b));
    assertFalse(ClassUtils.equal(null, b));
    assertFalse(ClassUtils.equal(b, a1));
    assertFalse(ClassUtils.equal(b, a2));
    assertFalse(ClassUtils.equal(b, null));
  }

  @Test
  public void testHash() {
    Object a = new HashBlob(1);
    Object b = new HashBlob(2);
    assertTrue(ClassUtils.hash(new Object[] {a, b, a, b}) == ClassUtils.hash(new Object[] {a, b, a, b}));
    assertFalse(ClassUtils.hash(new Object[] {a, b, a}) == ClassUtils.hash(new Object[] {a, b, a, b}));
    assertFalse(ClassUtils.hash(new Object[] {a, b, a, a}) == ClassUtils.hash(new Object[] {a, b, a, b}));
    assertFalse(ClassUtils.hash(new Object[] {b, a, b, a}) == ClassUtils.hash(new Object[] {a, b, a, b}));
  }

  @Test
  public void testClassTypesWithNullInArray() {
    Object[] array = new Object[] {"hello", null, "world"};
    Class<?>[] classTypes = ClassUtils.getClassTypes(array);
    assertEquals(3, classTypes.length);
    assertEquals(String.class, classTypes[0]);
    assertEquals(null, classTypes[1]);
    assertEquals(String.class, classTypes[2]);
  }

  @Test
  public void getFieldValue() throws Exception {
    final int hash = hashCode();
    HashBlob blob = new HashBlob(hash);
    assertThat(hash, equalTo(ClassUtils.getFieldValue(blob, "hash", false)));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getUnexistentFieldValue() throws Exception {
    ClassUtils.getFieldValue(new HashBlob(0), "fake", false);
  }

  @Test
  public void getFieldValueRecursive() throws Exception {
    final int hash = hashCode();
    HashBlob blob = new ExtendedHashBlob(hash);

    assertThat(hash, equalTo(ClassUtils.getFieldValue(blob, "hash", true)));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getUnexistentFieldValueRecursive() throws Exception {
    ClassUtils.getFieldValue(new ExtendedHashBlob(1), "fake", true);
  }

  @Test(expected = NoSuchFieldException.class)
  public void getInheritedFieldValueWithoutRecurse() throws Exception {
    ClassUtils.getFieldValue(new ExtendedHashBlob(1), "hash", false);
  }

  @Test
  public void setFieldValue() throws Exception {
    HashBlob blob = new HashBlob(0);
    final int hash = hashCode();

    ClassUtils.setFieldValue(blob, "hash", hash, false);

    assertThat(hash, equalTo(blob.getHash()));
  }

  @Test(expected = NoSuchFieldException.class)
  public void setUnexistentFieldValue() throws Exception {
    ClassUtils.setFieldValue(new HashBlob(0), "fake", 0, false);
  }

  @Test
  public void setFieldValueRecursive() throws Exception {
    HashBlob blob = new ExtendedHashBlob(0);
    final int hash = hashCode();

    ClassUtils.setFieldValue(blob, "hash", hash, true);
    assertThat(hash, equalTo(blob.getHash()));
  }

  @Test(expected = NoSuchFieldException.class)
  public void setUnexistentFieldValueRecursive() throws Exception {
    ClassUtils.setFieldValue(new ExtendedHashBlob(1), "fake", 0, true);
  }

  @Test(expected = NoSuchFieldException.class)
  public void setInheritedFieldValueWithoutRecurse() throws Exception {
    ClassUtils.setFieldValue(new ExtendedHashBlob(1), "hash", 0, false);
  }

  @Test
  public void isInstance() {
    assertThat(ClassUtils.isInstance(String.class, null), is(false));
    assertThat(ClassUtils.isInstance(String.class, ""), is(true));
    assertThat(ClassUtils.isInstance(Fruit.class, new Apple()), is(true));
    assertThat(ClassUtils.isInstance(Apple.class, new Kiwi()), is(false));

    assertThat(ClassUtils.isInstance(Integer.class, 0), is(true));
    assertThat(ClassUtils.isInstance(int.class, 0), is(true));
    assertThat(ClassUtils.isInstance(long.class, new Long(0)), is(true));
    assertThat(ClassUtils.isInstance(Double.class, new Double(0).doubleValue()), is(true));
    assertThat(ClassUtils.isInstance(double.class, new Double(0).doubleValue()), is(true));
    assertThat(ClassUtils.isInstance(boolean.class, true), is(true));
    assertThat(ClassUtils.isInstance(boolean.class, Boolean.TRUE), is(true));
    assertThat(ClassUtils.isInstance(Boolean.class, true), is(true));
    assertThat(ClassUtils.isInstance(String.class, true), is(false));
    assertThat(ClassUtils.isInstance(long.class, Boolean.FALSE), is(false));
  }

  @Test
  public void runWithClassLoader() {
    final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    final ClassLoader mockClassLoader = mock(ClassLoader.class);

    withContextClassLoader(mockClassLoader, () -> assertContextClassLoader(mockClassLoader));
    assertContextClassLoader(originalClassLoader);
  }

  @Test
  public void returnWithClassLoader() {
    final String value = "Hello World!";
    final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    final ClassLoader mockClassLoader = mock(ClassLoader.class);

    String response = withContextClassLoader(mockClassLoader, () -> {
      assertContextClassLoader(mockClassLoader);
      return value;
    });

    assertContextClassLoader(originalClassLoader);
    assertThat(response, is(value));
  }

  private void assertContextClassLoader(ClassLoader mockClassLoader) {
    assertThat(currentThread().getContextClassLoader(), is(sameInstance(mockClassLoader)));
  }

  private void simpleNameHelper(String target, Class clazz) {
    assertEquals(target, ClassUtils.getSimpleName(clazz));
  }

  private static class HashBlob {

    private int hash;

    public HashBlob(int hash) {
      this.hash = hash;
    }

    private int getHash() {
      return hash;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object other) {
      if (null == other || !getClass().equals(other.getClass())) {
        return false;
      }
      return hash == ((HashBlob) other).hash;
    }
  }

  private static class ExtendedHashBlob extends HashBlob {

    public ExtendedHashBlob(int hash) {
      super(hash);
    }
  }

}
