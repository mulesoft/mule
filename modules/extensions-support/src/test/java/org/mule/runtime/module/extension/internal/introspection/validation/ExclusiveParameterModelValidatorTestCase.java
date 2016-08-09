/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ExclusiveParameterModelValidatorTestCase extends AbstractMuleTestCase {

  private ModelValidator validator = new ExclusiveParameterModelValidator();
  private ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

  @Test
  public void validParameterTypes() throws Exception {
    validate(ValidExtension.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithNestedPojo() throws Exception {
    validate(InvalidExtensionWithNestedPojo.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithNestedGroup() throws Exception {
    validate(InvalidExtensionWithNestedGroup.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithOneOptionalParameter() throws Exception {
    validate(InvalidExtensionWithoOneOptionalParameters.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionWithNestedCollection() throws Exception {
    validate(InvalidExtensionWithNestedCollection.class);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidExclusionOperationParameter() throws Exception {
    validate(InvalidOperationExtension.class);
  }

  @Extension(name = "InvalidExtensionWithNestedCollection")
  public static class InvalidExtensionWithNestedCollection {

    @ParameterGroup
    private ExclusionWithNestedCollection group;
  }

  @Extension(name = "InvalidExtensionWithoOneOptionalParameters")
  public static class InvalidExtensionWithoOneOptionalParameters {

    @ParameterGroup
    private ExclusionWithoutOneOptionalParameters group;
  }

  @Extension(name = "InvalidExtensionWithNestedPojo")
  public static class InvalidExtensionWithNestedPojo {

    @ParameterGroup
    private ExclusionWithNestedPojo group;
  }

  @Extension(name = "InvalidExtensionWithNestedGroup")
  public static class InvalidExtensionWithNestedGroup {

    @ParameterGroup
    private ExclusionWithNestedParameterGroup group;
  }

  @Extension(name = "InvalidOperationExtension")
  @Operations({InvalidOperation.class})
  public static class InvalidOperationExtension {

  }


  @Extension(name = "ValidExtension")
  @Operations({ValidOperation.class})
  public static class ValidExtension {

    @ParameterGroup
    private ValidExclusion group;
  }



  @ExclusiveOptionals
  public static class ValidExclusion {

    @Parameter
    @Optional
    private String validType;

    @Parameter
    @Optional
    private String anotherValidType;
  }

  @ExclusiveOptionals
  public static class ExclusionWithNestedCollection {

    @Parameter
    private String validType;

    @Parameter
    private List<String> complexType;
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
  public static class ExclusionWithNestedParameterGroup {

    @Parameter
    @Optional
    private String validType;

    @Parameter
    @Optional
    private Integer otherValidType;

    @ParameterGroup
    private SimplePojo nesterGroup;
  }

  @ExclusiveOptionals
  public static class ExclusionWithoutOneOptionalParameters {

    @Parameter
    private String requiredParameter;

    @Parameter
    @Optional
    private Integer lonelyOptional;
  }

  public static class SimplePojo {

    @Parameter
    private Integer number;

  }

  public static class ValidOperation {

    public void validOperationWithExclusion(@ParameterGroup ValidExclusion exclusiveParameter) {

    }
  }

  public static class InvalidOperation {

    public void invalidOperationWithExclusion(@ParameterGroup ExclusionWithoutOneOptionalParameters invalidExclusionParameter) {

    }
  }

  private ExtensionModel modelFor(Class<?> connectorClass) {
    DescribingContext context = new DefaultDescribingContext(connectorClass.getClassLoader());
    return extensionFactory
        .createFrom(new AnnotationsBasedDescriber(connectorClass, new StaticVersionResolver(getProductVersion()))
            .describe(context), context);
  }

  private void validate(Class<?> connectorClass) {
    validator.validate(modelFor(connectorClass));
  }
}
