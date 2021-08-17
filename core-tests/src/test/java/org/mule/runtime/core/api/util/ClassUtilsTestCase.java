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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.isConcrete;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
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

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

@SmallTest
public class ClassUtilsTestCase extends AbstractMuleTestCase {

  // we do not want to match these methods when looking for a service method to
  // invoke
  protected final Set<String> ignoreMethods = new HashSet<String>(asList("equals", "getInvocationHandler"));

  @Test
  public void testIsConcrete() throws Exception {
    assertThat(isConcrete(Orange.class), is(true));
    assertThat(isConcrete(Fruit.class), is(false));
    assertThat(isConcrete(AbstractFruit.class), is(false));

    try {
      isConcrete(null);
      fail("Class cannot be null, exception should be thrown");
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void testLoadClass() throws Exception {
    Class clazz = loadClass("java.lang.String", getClass());
    assertThat(clazz, is(notNullValue()));
    assertThat(clazz.getName(), is("java.lang.String"));

    try {
      loadClass("java.lang.Bing", getClass());
      fail("ClassNotFoundException should be thrown");
    } catch (ClassNotFoundException e) {
      // expected
    }

  }

  @Test
  public void testLoadPrimitiveClass() throws Exception {
    assertThat(loadClass("boolean", getClass()), sameInstance(Boolean.TYPE));
    assertThat(loadClass("byte", getClass()), sameInstance(Byte.TYPE));
    assertThat(loadClass("char", getClass()), sameInstance(Character.TYPE));
    assertThat(loadClass("double", getClass()), sameInstance(Double.TYPE));
    assertThat(loadClass("float", getClass()), sameInstance(Float.TYPE));
    assertThat(loadClass("int", getClass()), sameInstance(Integer.TYPE));
    assertThat(loadClass("long", getClass()), sameInstance(Long.TYPE));
    assertThat(loadClass("short", getClass()), sameInstance(Short.TYPE));
  }

  @Test
  public void testLoadClassOfType() throws Exception {

    Class<? extends Exception> clazz = loadClass("java.lang.IllegalArgumentException", getClass(), Exception.class);
    assertThat(clazz, is(notNullValue()));

    assertThat(clazz.getName(), is("java.lang.IllegalArgumentException"));

    try {
      loadClass("java.lang.UnsupportedOperationException", getClass(), String.class);
      fail("IllegalArgumentException should be thrown since class is not of expected type");
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  @Test
  public void testInstanciateClass() throws Exception {
    Object object = instantiateClass("org.mule.tck.testmodels.fruit.Orange");
    assertThat(object, is(notNullValue()));
    assertThat(object, is(instanceOf(Orange.class)));

    object = instantiateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Apple(), new Banana());
    assertThat(object, is(notNullValue()));
    assertThat(object, is(instanceOf(FruitBowl.class)));

    FruitBowl bowl = (FruitBowl) object;

    assertThat(bowl.hasApple(), is(true));
    assertThat(bowl.hasBanana(), is(true));

    try {
      instantiateClass("java.lang.Bing");
      fail("Class does not exist, ClassNotFoundException should have been thrown");
    } catch (ClassNotFoundException e) {
      // expected
    }

  }

  @Test
  public void testGetParameterTypes() throws Exception {
    FruitBowl bowl = new FruitBowl();

    Class[] classes = ClassUtils.getParameterTypes(bowl, "apple");
    assertThat(classes, is(notNullValue()));
    assertThat(classes.length, is(equalTo(1)));
    assertThat(classes[0], is(equalTo(Apple.class)));


    classes = ClassUtils.getParameterTypes(bowl, "invalid");
    assertThat(classes, is(notNullValue()));
    assertThat(classes.length, is(equalTo(0)));
  }

  @Test
  public void testLoadingResources() throws Exception {
    URL resource = ClassUtils.getResource("log4j2-test.xml", getClass());
    assertThat(resource, is(notNullValue()));

    resource = ClassUtils.getResource("does-not-exist.properties", getClass());
    assertThat(resource, is(nullValue()));
  }

  @Test
  public void testLoadingResourceEnumeration() throws Exception {
    Enumeration enumeration = ClassUtils.getResources("log4j2-test.xml", getClass());
    assertThat(enumeration, is(notNullValue()));
    assertThat(enumeration.hasMoreElements(), is(true));

    enumeration = ClassUtils.getResources("does-not-exist.properties", getClass());
    assertThat(enumeration, is(notNullValue()));
    assertThat(enumeration.hasMoreElements(), is(false));
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
    assertThat(ClassUtils.equal(a1, a2), is(true));
    assertThat(ClassUtils.equal(b, b), is(true));
    assertThat(ClassUtils.equal(null, null), is(true));
    assertThat(ClassUtils.equal(a1, b), is(false));
    assertThat(ClassUtils.equal(a2, b), is(false));
    assertThat(ClassUtils.equal(null, b), is(false));
    assertThat(ClassUtils.equal(b, a1), is(false));
    assertThat(ClassUtils.equal(b, a2), is(false));
    assertThat(ClassUtils.equal(b, null), is(false));
  }

  @Test
  public void testHash() {
    Object a = new HashBlob(1);
    Object b = new HashBlob(2);
    assertThat(ClassUtils.hash(new Object[] {a, b, a, b}), is(equalTo(ClassUtils.hash(new Object[] {a, b, a, b}))));
    assertThat(ClassUtils.hash(new Object[] {a, b, a}), is(not(equalTo(ClassUtils.hash(new Object[] {a, b, a, b})))));
    assertThat(ClassUtils.hash(new Object[] {a, b, a, a}), is(not(equalTo(ClassUtils.hash(new Object[] {a, b, a, b})))));
    assertThat(ClassUtils.hash(new Object[] {b, a, b, a}), is(not(equalTo(ClassUtils.hash(new Object[] {a, b, a, b})))));
  }

  @Test
  public void testClassTypesWithNullInArray() {
    Object[] array = new Object[] {"hello", null, "world"};
    Class<?>[] classTypes = ClassUtils.getClassTypes(array);
    assertThat(classTypes.length, is(equalTo(3)));
    assertThat(classTypes[0], is(equalTo(String.class)));
    assertThat(classTypes[1], is(nullValue()));
    assertThat(classTypes[2], is(equalTo(String.class)));
  }


  @Test
  public void getField() throws Exception {
    Field field = ClassUtils.getField(HashBlob.class, "hash", false);
    assertThat(field, is(notNullValue()));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getNotExistentField() throws Exception {
    Field field = ClassUtils.getField(HashBlob.class, "wrongField", false);
  }

  @Test
  public void getFieldRecursive() throws Exception {
    Field field = ClassUtils.getField(ExtendedHashBlob.class, "hash", true);
    assertThat(field, is(notNullValue()));
  }


  @Test(expected = NoSuchFieldException.class)
  public void getNotExistentFieldRecursiveFieldInSuper() throws Exception {
    Field field = ClassUtils.getField(ExtendedHashBlob.class, "hash", false);
  }

  @Test(expected = NoSuchFieldException.class)
  public void getNotExistentFieldRecursive() throws Exception {
    Field field = ClassUtils.getField(ExtendedHashBlob.class, "wrongField", true);
  }

  @Test
  public void getFieldValue() throws Exception {
    final int hash = hashCode();
    HashBlob blob = new HashBlob(hash);
    assertThat(hash, is(equalTo(ClassUtils.getFieldValue(blob, "hash", false))));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getNotExistentFieldValue() throws Exception {
    ClassUtils.getFieldValue(new HashBlob(0), "wrongField", false);
  }

  @Test
  public void getFieldValueRecursive() throws Exception {
    final int hash = hashCode();
    HashBlob blob = new ExtendedHashBlob(hash);

    assertThat(hash, is(equalTo(ClassUtils.getFieldValue(blob, "hash", true))));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getNotExistentFieldValueRecursive() throws Exception {
    ClassUtils.getFieldValue(new ExtendedHashBlob(1), "fake", true);
  }

  @Test(expected = NoSuchFieldException.class)
  public void getInheritedFieldValueWithoutRecurse() throws Exception {
    ClassUtils.getFieldValue(new ExtendedHashBlob(1), "hash", false);
  }


  @Test
  public void getStaticFieldValueRecursive() throws Exception {
    final List<String> hashBlobProperties = asList("one", "two");
    ExtendedHashBlob.setStaticHashProperties(hashBlobProperties);
    List<String> value = ClassUtils.getStaticFieldValue(ExtendedHashBlob.class, "staticHashProperties", true);
    assertThat(value, equalTo(hashBlobProperties));
  }

  @Test(expected = NoSuchFieldException.class)
  public void getStaticFieldValueNotExistentField() throws Exception {
    ClassUtils.getStaticFieldValue(ExtendedHashBlob.class, "fake", false);
  }

  @Test(expected = NoSuchFieldException.class)
  public void getStaticFieldValueNotExistentFieldRecursive() throws Exception {
    ClassUtils.getStaticFieldValue(ExtendedHashBlob.class, "fake", true);
  }


  @Test(expected = IllegalAccessException.class)
  public void getStaticFieldValueNotStaticField() throws Exception {
    ClassUtils.getStaticFieldValue(HashBlob.class, "hash", false);
  }

  @Test
  public void setFieldValue() throws Exception {
    HashBlob blob = new HashBlob(0);
    final int hash = hashCode();

    ClassUtils.setFieldValue(blob, "hash", hash, false);
    assertThat(hash, is(equalTo(blob.getHash())));
  }

  @Test
  public void setFinalFieldValue() throws Exception {
    final int originalHash = hashCode();
    final List<String> newFinalHashProperties = asList("one", "two");
    HashBlob blob = new HashBlob(originalHash);
    ClassUtils.setFieldValue(blob, "finalHashProperties", newFinalHashProperties, false);
    assertThat(newFinalHashProperties, equalTo(blob.getFinalHashProperties()));
  }


  @Test
  public void setFinalFieldValueRecursive() throws Exception {
    final int originalHash = hashCode();
    final List<String> newFinalHashProperties = asList("one", "two");
    ExtendedHashBlob blob = new ExtendedHashBlob(originalHash);
    ClassUtils.setFieldValue(blob, "finalHashProperties", newFinalHashProperties, true);
    assertThat(newFinalHashProperties, equalTo(blob.getFinalHashProperties()));
  }

  @Test
  public void setFinalStaticFieldValue() throws Exception {
    final List<String> newFinalStaticHashProperties = asList("one", "two");
    ClassUtils.setStaticFieldValue(HashBlob.class, "finalStaticHashProperties", newFinalStaticHashProperties, true);
    assertThat(newFinalStaticHashProperties, equalTo(HashBlob.getFinalStaticHashProperties()));
  }

  @Test
  public void setFinalStaticFieldRecursive() throws Exception {
    final List<String> newFinalStaticHashProperties = asList("one", "two");
    ClassUtils.setStaticFieldValue(ExtendedHashBlob.class, "finalStaticHashProperties", newFinalStaticHashProperties, true);
    assertThat(newFinalStaticHashProperties, equalTo(ExtendedHashBlob.getFinalStaticHashProperties()));
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
    assertThat(ClassUtils.getSimpleName(clazz), is(equalTo(target)));
  }

  private static class HashBlob {

    private int hash;
    private static List<String> staticHashProperties = new ArrayList<>();
    private final List<String> finalHashProperties = asList("one", "two");
    private final static List<String> finalStaticHashProperties = asList("one", "two");


    public HashBlob(int hash) {
      this.hash = hash;
    }

    private int getHash() {
      return hash;
    }

    public static void setStaticHashProperties(List<String> properties) {
      staticHashProperties = properties;
    }

    public static List<String> getFinalStaticHashProperties() {
      return finalStaticHashProperties;
    }

    public List<String> getFinalHashProperties() {
      return finalHashProperties;
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
