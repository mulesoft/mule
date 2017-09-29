/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional.compatibility;

import static java.util.Arrays.asList;
import org.junit.runners.Parameterized;
import org.mule.test.functional.ModuleWithGlobalElementTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class CompatibilityModuleWithGlobalElementTestCase extends ModuleWithGlobalElementTestCase {

  @Parameterized.Parameters(name = "{index}: Running tests for {0} (validating XML [{2}]) ")
  public static Collection<Object[]> data() {
    return asList(simpleScenario(true),
                  simpleScenario(false),
                  literalAndExpressionScenario(true),
                  literalAndExpressionScenario(false));
  }

  private static Object[] simpleScenario(boolean shouldValidate) {
    //simple scenario
    return new Object[] {"compatibility/flows/flows-using-module-global-elements.xml",
        new String[] {"compatibility/" + MODULE_GLOBAL_ELEMENT_XML},
        shouldValidate};
  }

  private static Object[] literalAndExpressionScenario(boolean shouldValidate) {
    //using literals and expressions that will be resolved accordingly scenario
    return new Object[] {"compatibility/flows/flows-using-module-global-elements-with-expressions.xml",
        new String[] {MODULE_GLOBAL_ELEMENT_XML}, shouldValidate};
  }

}
