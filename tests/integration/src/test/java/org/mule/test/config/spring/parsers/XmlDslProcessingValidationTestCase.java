/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.rules.ExpectedException.none;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XmlDslProcessingValidationTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void parameterAndChildAtOnce() throws Exception {
    expectedException
        .expectMessage("Component parsers-test:element-with-attribute-and-child has a child element parsers-test:my-pojo which is used for the same purpose of the configuration parameter myPojo. Only one must be used.");
    new ApplicationContextBuilder().setApplicationResources(new String[] {
        "org/mule/config/spring/parsers/dsl-validation-duplicate-pojo-or-list-parameter-config.xml"}).build();
  }

  @Test
  public void namelessTopLevelElement() throws Exception {
    expectedException.expectMessage("Global element parsers-test:parameter-collection-parser does not provide a name attribute.");
    new ApplicationContextBuilder().setApplicationResources(new String[] {
        "org/mule/config/spring/parsers/dsl-validation-nameless-top-level-element-config.xml"}).build();
  }

}
