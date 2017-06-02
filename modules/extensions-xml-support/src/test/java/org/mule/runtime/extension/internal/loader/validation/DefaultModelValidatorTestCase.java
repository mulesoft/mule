/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;

import static java.util.Collections.emptySet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import org.apache.maven.model.validation.ModelValidator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the defaults {@link ModelValidator}s provided by the {@link ExtensionModelFactory}
 *
 * @since 4.0
 */
@SmallTest
public class DefaultModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    exception.expect(IllegalModelDefinitionException.class);
  }

  @Test
  public void repeatedParameterNamesThrowsException() {
    exception.expectMessage("repeated-parameter");
    getExtensionModelFrom("validation/module-repeated-parameters.xml");
  }

  @Test
  public void repeatedOperationNamesThrowsException() {
    exception.expectMessage("repeated-operation");
    getExtensionModelFrom("validation/module-repeated-operations.xml");
  }

  @Test
  public void parameterWithRequiredAndDefaultThrowsException() {
    exception.expectMessage("aWrongDefinedParameter");
    getExtensionModelFrom("validation/module-parameter-required-default.xml");
  }

  @Test
  public void propertyWithRequiredAndDefaultThrowsException() {
    exception.expectMessage("aWrongDefinedProperty");
    getExtensionModelFrom("validation/module-property-required-default.xml");
  }

  @Test
  public void wrongNamingForXmlThrowsException() {
    exception.expectMessage("property with spaces");
    getExtensionModelFrom("validation/module-not-xml-valid-names.xml");
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(emptySet()), parameters);
  }

}
