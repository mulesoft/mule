/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_SET;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.BOOLEAN;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.NUMBER;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.STRING;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.test.allure.AllureConstants.XmlSdk.Declaration.DECLARATION_DATASENSE;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.api.loader.xml.declaration.DeclarationOperation;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(Parameterized.class)
@Feature(XML_SDK)
@Story(DECLARATION_DATASENSE)
public class XmlExtensionLoaderWithDeclarationTestCase extends AbstractMuleTestCase {

  private static final String MODULE_DECLARATION_RESOURCE_FILE = "modules/declaration/module-declarer-datasense.xml";
  private static final MetadataType VOID_TYPE = BaseTypeBuilder.create(JAVA).voidType().build();
  private static final MetadataType STRING_TYPE = PRIMITIVE_TYPES.get(STRING);
  private static final MetadataType NUMBER_TYPE = PRIMITIVE_TYPES.get(NUMBER);
  private static final MetadataType ANY_TYPE = PRIMITIVE_TYPES.get(PrimitiveTypesTypeLoader.ANY);
  private static final MetadataType BOOLEAN_TYPE = PRIMITIVE_TYPES.get(BOOLEAN);
  private static final String DOC_OUTPUT_FORMAT = "Documentation for the output [%s]";
  private static final String DOC_OUTPUT_ATTRIBUTE_FORMAT = "Documentation for the output-attribute [%s]";

  private final boolean validateXml;

  @Parameterized.Parameters(name = "Validate XML: {0}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false},
        {true}
    });
  }

  /**
   * @param validateXml whether the XML must be valid while loading the extension model or not. Useful to determine if the default
   *        values are properly feed when reading the document.
   */
  public XmlExtensionLoaderWithDeclarationTestCase(boolean validateXml) {
    this.validateXml = validateXml;
  }

  @Test
  public void testModuleWithoutDeclaration_expectsNoRemapping() {
    assertDefaultModule(empty());
  }

  @Test
  public void testModuleWrongPathDeclaration_expectsNoRemapping() {
    assertDefaultModule(of("non/existing/folder/non-existing-path-declaration.json"));
  }

  @Test
  public void testModuleWrongOperationsDeclaration_expectsNoRemapping() {
    assertDefaultModule(of("modules/declaration/declaration-no-correct-mapping.json"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testModuleWrongFormatDeclaration_expectsThrowException() {
    getExtensionModelFrom(of("modules/declaration/declaration-no-correct-format.json"));
  }

  @Test
  public void testModuleSingleOperationRemapping_expectsOneRemappedOperation() {
    final DeclarationOperation declaresOutput =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.JSON).binaryType().build(),
                                 BaseTypeBuilder.create(MetadataFormat.XML).numberType().build());
    final DeclarationOperation declaresAttributes = new DeclarationOperation(VOID_TYPE, NUMBER_TYPE);
    final DeclarationOperation declaresOutputAndAttributes = new DeclarationOperation(ANY_TYPE, BOOLEAN_TYPE);
    final DeclarationOperation declaresNothing = new DeclarationOperation(VOID_TYPE, VOID_TYPE);
    assertModule(of("modules/declaration/declaration-one-operation.json"), declaresOutput, declaresAttributes,
                 declaresOutputAndAttributes, declaresNothing);
  }

  @Test
  public void testModuleDoubleOperationRemapping_expectsTwoRemappedOperation() {
    final DeclarationOperation declaresOutput =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.JSON).binaryType().build(),
                                 BaseTypeBuilder.create(MetadataFormat.XML).numberType().build());
    final DeclarationOperation declaresAttributes = new DeclarationOperation(VOID_TYPE, NUMBER_TYPE);
    final DeclarationOperation declaresOutputAndAttributes =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.XML).numberType().build(), VOID_TYPE);
    final DeclarationOperation declaresNothing = new DeclarationOperation(VOID_TYPE, VOID_TYPE);
    assertModule(of("modules/declaration/declaration-two-operations.json"), declaresOutput, declaresAttributes,
                 declaresOutputAndAttributes, declaresNothing);
  }

  @Test
  public void testModuleAllOperationRemapping_expectsAllRemappedOperation() {
    final DeclarationOperation declaresOutput =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.JSON).binaryType().build(),
                                 BaseTypeBuilder.create(MetadataFormat.XML).numberType().build());
    final DeclarationOperation declaresAttributes =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.XML).dateType().build(),
                                 BaseTypeBuilder.create(MetadataFormat.CSV).numberType().build());
    final DeclarationOperation declaresOutputAndAttributes =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.XML).numberType().build(), VOID_TYPE);
    final DeclarationOperation declaresNothing =
        new DeclarationOperation(BaseTypeBuilder.create(MetadataFormat.JSON).stringType().build(),
                                 BaseTypeBuilder.create(MetadataFormat.CSV).stringType().build());
    assertModule(of("modules/declaration/declaration-all-operations.json"), declaresOutput, declaresAttributes,
                 declaresOutputAndAttributes, declaresNothing);
  }

  private void assertDefaultModule(Optional<String> declarationPath) {
    final DeclarationOperation declaresOutput = new DeclarationOperation(STRING_TYPE, VOID_TYPE);
    final DeclarationOperation declaresAttributes = new DeclarationOperation(VOID_TYPE, NUMBER_TYPE);
    final DeclarationOperation declaresOutputAndAttributes = new DeclarationOperation(ANY_TYPE, BOOLEAN_TYPE);
    final DeclarationOperation declaresNothing = new DeclarationOperation(VOID_TYPE, VOID_TYPE);
    assertModule(declarationPath, declaresOutput, declaresAttributes, declaresOutputAndAttributes, declaresNothing);
  }

  private void assertModule(Optional<String> declarationPath, DeclarationOperation declaresOutput,
                            DeclarationOperation declaresAttributes,
                            DeclarationOperation declaresOutputAndAttributes, DeclarationOperation declaresNothing) {
    final ExtensionModel extensionModel = getExtensionModelFrom(declarationPath);
    assertThat(extensionModel.getName(), is("module-declarer-datasense"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(4));

    assertOperation("op-declares-output", extensionModel, declaresOutput, format(DOC_OUTPUT_FORMAT, "op-declares-output"), "");
    assertOperation("op-declares-attributes", extensionModel, declaresAttributes, "",
                    format(DOC_OUTPUT_ATTRIBUTE_FORMAT, "op-declares-attributes"));
    assertOperation("op-declares-output-and-attributes", extensionModel, declaresOutputAndAttributes,
                    format(DOC_OUTPUT_FORMAT, "op-declares-output-and-attributes"),
                    format(DOC_OUTPUT_ATTRIBUTE_FORMAT, "op-declares-output-and-attributes"));
    assertOperation("op-declares-nothing", extensionModel, declaresNothing, "", "");
  }

  private void assertOperation(String operationName, ExtensionModel extensionModel, DeclarationOperation declarer,
                               String outputDocumentation, String outputAttributeDocumentation) {
    final Optional<OperationModel> operationModelOptional = extensionModel.getOperationModel(operationName);
    assertThat(operationModelOptional.isPresent(), is(true));
    final OperationModel operationModel = operationModelOptional.get();
    assertThat(operationModel.getOutput().getType(), is(declarer.getOutput()));
    assertThat(operationModel.getOutput().getDescription(), is(outputDocumentation));

    assertThat(operationModel.getOutputAttributes().getType(), is(declarer.getOutputAttributes()));
    assertThat(operationModel.getOutputAttributes().getDescription(), is(outputAttributeDocumentation));
  }

  private ExtensionModel getExtensionModelFrom(Optional<String> declarationPath) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, MODULE_DECLARATION_RESOURCE_FILE);
    parameters.put(XmlExtensionModelLoader.VALIDATE_XML, validateXml);
    declarationPath.ifPresent(path -> parameters.put(XmlExtensionModelLoader.RESOURCE_DECLARATION, path));
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(),
                                                            getDefault(EMPTY_SET),
                                                            parameters);
  }
}
