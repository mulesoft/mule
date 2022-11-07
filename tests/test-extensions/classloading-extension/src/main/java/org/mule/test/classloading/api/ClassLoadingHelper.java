/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.StringContains.containsString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;

public class ClassLoadingHelper {

  public static Map<String, ClassLoader> createdClassLoaders = new HashMap<>();

  public static void addClassLoader(String element) {
    createdClassLoaders.put(element, Thread.currentThread().getContextClassLoader());
  }

  public static void verifyUsedClassLoaders(String... phasesToExecute) {
    Map<String, ClassLoader> createdClassLoaders = ClassLoadingHelper.createdClassLoaders;
    List<ClassLoader> collect = createdClassLoaders.values().stream().distinct().collect(toList());
    collect.forEach(ClassLoadingHelper::assertExtensionClassLoader);
    Set<String> executedPhases = createdClassLoaders.keySet();
    assertThat(executedPhases, is(hasItems(stream(phasesToExecute).map(StringContains::containsString).toArray(Matcher[]::new))));
  }

  private static void assertExtensionClassLoader(ClassLoader classLoader) {
    assertThat(classLoader.toString(),
               allOf(containsString("classloading-extension"),
                     anyOf(containsString(".TestRegionClassLoader[Region] @"),
                           containsString("MulePluginClassLoader"))));
  }
}
