/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.RAISE_ERROR_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.CORE_ERROR_NS;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.TARGET_TYPE;
import static org.mule.runtime.config.internal.model.ApplicationModel.ERROR_MAPPING_IDENTIFIER;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.validator.CorrectPrefixesValidator.EMPTY_TYPE_FORMAT_MESSAGE;
import static org.mule.runtime.extension.internal.loader.validator.CorrectPrefixesValidator.TYPE_RAISE_ERROR_ATTRIBUTE;
import static org.mule.runtime.extension.internal.loader.validator.CorrectPrefixesValidator.WRONG_VALUE_FORMAT_MESSAGE;
import static org.mule.runtime.extension.internal.loader.validator.GlobalElementNamesValidator.ILLEGAL_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE;
import static org.mule.runtime.extension.internal.loader.validator.GlobalElementNamesValidator.REPEATED_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.maven.model.validation.ModelValidator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
    exception.expectMessage(allOf(
                                  containsString("[operation with spaces] is not a valid one"),
                                  containsString("[parameters with spaces] is not a valid one"),
                                  containsString("[property with spaces] is not a valid one")));
    getExtensionModelFrom("validation/module-not-xml-valid-names.xml");
  }

  @Test
  public void emptyTypeInRaiseErrorThrowsException() {
    exception.expectMessage(allOf(
                                  containsString(format(
                                                        EMPTY_TYPE_FORMAT_MESSAGE,
                                                        RAISE_ERROR_IDENTIFIER.toString(),
                                                        TYPE_RAISE_ERROR_ATTRIBUTE,
                                                        "fail-raise-error")),
                                  containsString(format(
                                                        EMPTY_TYPE_FORMAT_MESSAGE,
                                                        RAISE_ERROR_IDENTIFIER.toString(),
                                                        TYPE_RAISE_ERROR_ATTRIBUTE,
                                                        "fail-raise-error-nested"))));
    getExtensionModelFrom("validation/module-using-raise-error-empty-type.xml");
  }

  @Test
  public void wrongTypeInRaiseErrorNestedThrowsException() {
    exception.expectMessage(allOf(
                                  containsString(format(
                                                        WRONG_VALUE_FORMAT_MESSAGE,
                                                        RAISE_ERROR_IDENTIFIER.toString(),
                                                        TYPE_RAISE_ERROR_ATTRIBUTE,
                                                        CORE_ERROR_NS,
                                                        "MODULE-USING-RAISE-ERROR",
                                                        "WRONG-PREFIX",
                                                        "fail-raise-error")),
                                  containsString(format(
                                                        WRONG_VALUE_FORMAT_MESSAGE,
                                                        RAISE_ERROR_IDENTIFIER.toString(),
                                                        TYPE_RAISE_ERROR_ATTRIBUTE,
                                                        CORE_ERROR_NS,
                                                        "MODULE-USING-RAISE-ERROR",
                                                        "WRONG-PREFIX",
                                                        "fail-raise-error-nested"))));
    getExtensionModelFrom("validation/module-using-raise-error-wrong-type.xml");
  }

  @Test
  public void emptyTargetTypeInErrorMappingThrowsException() {
    exception.expectMessage(allOf(
                                  containsString(format(
                                                        EMPTY_TYPE_FORMAT_MESSAGE,
                                                        ERROR_MAPPING_IDENTIFIER.toString(),
                                                        TARGET_TYPE,
                                                        "fail-raise-error")),
                                  containsString(format(
                                                        EMPTY_TYPE_FORMAT_MESSAGE,
                                                        ERROR_MAPPING_IDENTIFIER.toString(),
                                                        TARGET_TYPE,
                                                        "fail-raise-error-nested"))));
    getExtensionModelFrom("validation/module-using-errormapping-empty-targetType.xml", getDependencyExtensions());
  }

  @Test
  public void wrongTargetTypeInErrorMappingNestedThrowsException() {
    exception.expectMessage(allOf(
                                  containsString(format(
                                                        WRONG_VALUE_FORMAT_MESSAGE,
                                                        ERROR_MAPPING_IDENTIFIER.toString(),
                                                        TARGET_TYPE,
                                                        CORE_ERROR_NS,
                                                        "MODULE-USING-ERRORMAPPING",
                                                        "WRONG-PREFIX",
                                                        "fail-raise-error")),
                                  containsString(format(
                                                        WRONG_VALUE_FORMAT_MESSAGE,
                                                        ERROR_MAPPING_IDENTIFIER.toString(),
                                                        TARGET_TYPE,
                                                        CORE_ERROR_NS,
                                                        "MODULE-USING-ERRORMAPPING",
                                                        "WRONG-PREFIX",
                                                        "fail-raise-error-nested"))));
    getExtensionModelFrom("validation/module-using-errormapping-wrong-targetType.xml", getDependencyExtensions());
  }

  @Test
  public void wrongGlobalElementNamesThrowsException() {
    exception.expectMessage(allOf(
                                  containsString(format(
                                                        REPEATED_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE,
                                                        "repeated-config-name",
                                                        "petstore:config",
                                                        "petstore:config")),
                                  containsString(format(
                                                        REPEATED_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE,
                                                        "repeated-config-name",
                                                        "petstore:config",
                                                        "heisenberg:config")),
                                  containsString(format(
                                                        ILLEGAL_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE,
                                                        "ilegal-petstore-config-name_lal[\\{#a",
                                                        ""))));
    getExtensionModelFrom("validation/module-repeated-global-elements.xml", getDependencyExtensions());
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    return getExtensionModelFrom(modulePath, emptySet());
  }

  private ExtensionModel getExtensionModelFrom(String modulePath, Set<ExtensionModel> extensions) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(extensions), parameters);
  }

  private Set<ExtensionModel> getDependencyExtensions() {
    ExtensionModel petstore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel heisenberg = loadExtension(HeisenbergExtension.class, emptySet());
    return ImmutableSet.<ExtensionModel>builder().add(petstore, heisenberg).build();
  }

  private ExtensionModel loadExtension(Class extension, Set<ExtensionModel> deps) {
    DefaultJavaExtensionModelLoader loader = new DefaultJavaExtensionModelLoader();
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extension.getName());
    ctx.put(VERSION, "1.0.0-SNAPSHOT");
    return loader.loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps), ctx);
  }

}
