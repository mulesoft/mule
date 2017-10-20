/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Test;

public class AnnotationUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void readAnnotationFromClass() throws Exception {
    Class configuredClass = AnnotationUtils.findConfiguredClass(C.class);

    assertThat(configuredClass, equalTo(C.class));
  }

  @Test
  public void readAnnotationFromSuperClassIfNoDirectlyImplementedAnnotatedInterface() throws Exception {
    Class configuredClass = AnnotationUtils.findConfiguredClass(B.class);

    assertThat(configuredClass, equalTo(A.class));
  }

  @Test
  public void readAnnotationFromDirectlyImplementedAnnotatedInterface() throws Exception {
    Class configuredClass = AnnotationUtils.findConfiguredClass(E.class);

    assertThat(configuredClass, equalTo(Interface3.class));
  }

  @Test
  public void readAnnotationFromSuperInterface() throws Exception {
    Class configuredClass = AnnotationUtils.findConfiguredClass(D.class);

    assertThat(configuredClass, equalTo(Interface1.class));
  }

  @ArtifactClassLoaderRunnerConfig
  public static class A {

  }

  public static class B extends A implements Interface2 {

  }

  @ArtifactClassLoaderRunnerConfig
  public static class C extends B {

  }

  @ArtifactClassLoaderRunnerConfig
  public interface Interface1 {

  }

  public interface Interface2 extends Interface1 {

  }

  @ArtifactClassLoaderRunnerConfig
  public interface Interface3 {

  }

  public static class D implements Interface2 {

  }

  public static class E extends A implements Interface3 {

  }
}
