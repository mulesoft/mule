/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelFactory;
import org.mule.metadata.api.utils.MetadataTypeEnricher;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(PARAMETERS)
public class MuleSdkParameterModelParserConfigNamesTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private MuleSdkParameterModelParserSdkBuilder baseParameterParserBuilder;
  private MetadataType someValidMetadataType = mock(MetadataType.class);

  @Before
  public void setUp() {
    someValidMetadataType = BaseTypeBuilder.create(JAVA).anyType().build();

    final Map<String, MetadataType> typeLoaderTypes = new HashMap<>();
    typeLoaderTypes.put("somevalid", someValidMetadataType);
    typeLoaderTypes.put("some:some-config-colliding-with-type", someValidMetadataType);

    baseParameterParserBuilder = new MuleSdkParameterModelParserSdkBuilder("someparam", "somevalid")
        .withTypeLoaderTypes(unmodifiableMap(typeLoaderTypes))
        .withExtensionInContext(buildExtensionDeclarer());
  }

  private ExtensionDeclarer buildExtensionDeclarer() {
    // We want to use an explicit prefix that is different from the name, to make sure the correct one is used
    XmlDslModel xmlDslModel = XmlDslModel.builder().setPrefix("some").build();

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    extensionDeclarer
        .named("Some Extension")
        .onVersion("1.0.0")
        .fromVendor("Mulesoft")
        .withCategory(COMMUNITY)
        .withXmlDsl(xmlDslModel)
        .withConstruct("some-construct");

    extensionDeclarer.withConfig("some-config");
    extensionDeclarer.withConfig("some-config-colliding-with-type");

    return extensionDeclarer;
  }

  @Test
  @Description("Checks that when the parameter type matches a configuration from an extension, it is treated as a configuration name")
  public void parameterTypeMatchingConfigurationFromOtherExtensionIsTreatedAsReference() {
    StereotypeModel expectedStereotype = newStereotype("some-config", "SOME").withParent(CONFIG).build();
    MetadataType expectedType = BaseTypeBuilder.create(JAVA).objectType()
        .id(ConfigurationProvider.class.getName())
        .with(new ClassInformationAnnotation(ConfigurationProvider.class))
        .build();
    MuleSdkParameterModelParser parameterModelParser = baseParameterParserBuilder.withType("some:some-config").build();
    assertThat(parameterModelParser.getType(), is(expectedType));
    assertThat(parameterModelParser.getAllowedStereotypes(mock(StereotypeModelFactory.class)), contains(expectedStereotype));
  }

  @Test
  @Description("Checks that when the parameter type matches the namespace of an extension but not an actual configuration, it is treated as invalid")
  public void parameterTypeMatchingExtensionPrefixButNoConfigRaisesException() {
    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references unknown type 'some:invalid'");
    baseParameterParserBuilder.withType("some:invalid").build();
  }

  @Test
  @Description("Checks that when the parameter type matches a configuration on an extension but also a type in the type loader, the type loader takes precedence. Whether that situation is possible or not, is of no concern to the parser, hence the test")
  public void parameterTypeMatchingExtensionConfigCollidingWithTypeInTypeLoaderPrioritizesTypeLoader() {
    MuleSdkParameterModelParser parameterModelParser =
        baseParameterParserBuilder.withType("some:some-config-colliding-with-type").build();
    MetadataType expectedType =
        new MetadataTypeEnricher().enrich(someValidMetadataType, singleton(new TypedValueTypeAnnotation()));
    assertThat(parameterModelParser.getType(), is(expectedType));
  }

  @Test
  @Description("Checks that when the parameter type matches a component of an extension which is not a configuration, it is treated as invalid")
  public void parameterTypeMatchingExtensionConstructRaisesException() {
    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references unknown type 'some:some-construct'");
    baseParameterParserBuilder.withType("some:some-construct").build();
  }
}
