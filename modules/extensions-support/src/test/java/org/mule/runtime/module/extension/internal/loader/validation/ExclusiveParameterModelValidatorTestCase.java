/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.internal.loader.validator.ExclusiveParameterModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ExclusiveParameterModelValidatorTestCase extends AbstractMuleTestCase {

  private ExtensionModelValidator validator = new ExclusiveParameterModelValidator();

  @Test
  public void validParameterTypes() throws Exception {
    validate(ValidExtension.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithOneOptionalParameter() throws Exception {
    validate(InvalidExtensionWithoOneOptionalParameters.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithoutOptionals() throws Exception {
    validate(InvalidExtensionWithoutOptionals.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionOperationParameter() throws Exception {
    validate(InvalidOperationExtension.class);
  }

  @Extension(name = "InvalidExtensionWithNestedCollection")
  public static class InvalidExtensionWithoutOptionals {

    @ParameterGroup(name = "exclusion")
    private ExclusionWithNestedCollection group;
  }

  @Extension(name = "InvalidExtensionWithoOneOptionalParameters")
  public static class InvalidExtensionWithoOneOptionalParameters {

    @ParameterGroup(name = "exclusion")
    private ExclusionWithoutOneOptionalParameters group;
  }

  @Extension(name = "InvalidOperationExtension")
  @Operations({InvalidOperation.class})
  public static class InvalidOperationExtension {

  }


  @Extension(name = "ValidExtension")
  @Operations({ValidOperation.class})
  public static class ValidExtension {

    @ParameterGroup(name = "exclusion")
    private ValidExclusion group;
  }

  @ExclusiveOptionals
  public static class ValidExclusion {

    @Parameter
    @Optional
    private String validType;

    @Parameter
    @Optional
    private String domain;

    @Parameter
    @Optional
    private String url;

    @Parameter
    private Integer number;
  }

  @ExclusiveOptionals
  public static class ExclusionWithNestedCollection {

    @Parameter
    private String validType;

    @Parameter
    private List<String> complexTypes;
  }

  @ExclusiveOptionals
  public static class ExclusionWithNestedPojo {

    @Parameter
    @Optional
    private String validType;

    @Parameter
    @Optional
    private SimplePojo complexField;

    // Required parameters should be ignored
    @Parameter
    private SimplePojo requiredPojo;
  }

  @ExclusiveOptionals
  public static class ExclusionWithoutOneOptionalParameters {

    @Parameter
    private String requiredParameter;

    @Parameter
    @Optional
    private Integer lonelyOptional;
  }

  @ExclusiveOptionals
  public static class ExclusivePojo {

    @Parameter
    private Integer number;

  }

  public static class SimplePojo {

    @Parameter
    private Integer number;
  }

  public static class ValidOperation {

    public void validOperationWithExclusion(@ParameterGroup(name = "exclusion") ValidExclusion exclusiveParameter) {

    }
  }

  public static class InvalidOperation {

    public void invalidOperationWithExclusion(@ParameterGroup(
        name = "exclusion") ExclusionWithoutOneOptionalParameters invalidExclusionParameter) {

    }
  }

  private void validate(Class<?> connectorClass) {
    ExtensionsTestUtils.validate(connectorClass, validator);
  }
}
