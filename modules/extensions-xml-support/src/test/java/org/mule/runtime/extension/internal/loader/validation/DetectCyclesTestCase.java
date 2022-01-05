/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.VALIDATE_XML;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableSet;

/**
 * Tests if the current module contains cycles with its TNS references through operations, and fails accordingly.
 *
 * @since 4.0
 */
@SmallTest
@RunWith(Parameterized.class)
public class DetectCyclesTestCase extends AbstractMuleTestCase {

  private final boolean validateXml;

  @Parameterized.Parameters(name = "Detecting cycles validating XML: {0}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false},
        {true}
    });
  }

  /**
   * @param validateXml whether the XML must be valid while loading the extension model or not. Useful to determine if the default
   *                    values are properly feed when reading the document.
   */
  public DetectCyclesTestCase(boolean validateXml) {
    this.validateXml = validateXml;
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    exception.expect(MuleRuntimeException.class);
  }

  @Test
  public void simpleCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple-cycle.xml", getDependencyExtensions(), "op1", "op2");
  }

  @Test
  public void simpleCyclesInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple2-cycle.xml", getDependencyExtensions(), "op1", "op2", "op3");
  }

  @Test
  public void simpleCyclesInOperationsThroughTNSPrivateThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple3-cycle.xml", getDependencyExtensions(), "op1", "internal-op2",
                          "internal-op3");
  }

  @Test
  public void nestedCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-nested-foreach-cycle.xml", getDependencyExtensions(), "foreach-op1",
                          "foreach-op2");
  }

  @Test
  public void simpleRecursiveCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple-recursive-cycle.xml", getDependencyExtensions(), "op1");
  }

  private Set<ExtensionModel> getDependencyExtensions() {
    return ImmutableSet.<ExtensionModel>builder().add(getExtensionModel()).build();
  }

  private ExtensionModel getExtensionModelFrom(String modulePath, Set<ExtensionModel> extensions, String... offendingOperations) {
    exception.expectMessage(format(XmlExtensionLoaderDelegate.CYCLIC_OPERATIONS_ERROR,
                                   new TreeSet(new HashSet<>(Arrays.asList(offendingOperations))).toString()));
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    parameters.put(VALIDATE_XML, validateXml);
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    parameters.put("COMPILATION_MODE", true);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(extensions), parameters);
  }
}
