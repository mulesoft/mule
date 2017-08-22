/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

/**
 * Tests if the current module contains cycles with its TNS references through operations, and fails accordingly.
 *
 * @since 4.0
 */
@SmallTest
public class DetectCyclesTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    exception.expect(MuleRuntimeException.class);
  }

  @Test
  public void simpleCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple-cycle.xml", "op1", "op2");
  }

  @Test
  public void simpleCyclesInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple2-cycle.xml", "internal-op1", "internal-op2", "internal-op3");
  }

  @Test
  public void nestedCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-nested-foreach-cycle.xml", "foreach-op1", "foreach-op2");
  }

  @Test
  public void simpleRecursiveCycleInOperationsThroughTNSThrowsException() {
    getExtensionModelFrom("validation/cycle/module-simple-recursive-cycle.xml", "op1");
  }

  private ExtensionModel getExtensionModelFrom(String modulePath, String... offendingOperations) {
    exception.expectMessage(format(XmlExtensionLoaderDelegate.CYCLIC_OPERATIONS_ERROR,
                                   new TreeSet(new HashSet<>(Arrays.asList(offendingOperations))).toString()));
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(emptySet()), parameters);
  }
}
