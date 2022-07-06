package org.mule.runtime.core.api.extension;

import static org.mule.runtime.api.util.MuleSystemProperties.SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;

import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class MuleExtensionModelDeclarerTestCase {

  private String systemPropertyName;
  private String systemPropertyValue;
  private String systemPropertyOldValue;
  private String expectedResult;

  public MuleExtensionModelDeclarerTestCase(String supportExtressionsInVariableNameInSetVariable, String expectedResult) {
    systemPropertyName = SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY;
    systemPropertyValue = supportExtressionsInVariableNameInSetVariable;
    this.expectedResult = expectedResult;
  }

  @Parameterized.Parameters(name = "Support expressions in variableName in SetVariable: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"true", "SUPPORTED"},
        {"false", "NOT_SUPPORTED"}
    });
  }

  @Before
  public void setUp() {
    systemPropertyOldValue = System.setProperty(systemPropertyName, systemPropertyValue);
    MuleExtensionModelDeclarer.refreshSystemProperties();
  }

  @After
  public void tearDown() {
    if (systemPropertyOldValue == null) {
      System.clearProperty(systemPropertyName);
    } else {
      System.setProperty(systemPropertyName, systemPropertyOldValue);
    }
    MuleExtensionModelDeclarer.refreshSystemProperties();
  }

  @Test
  @Issue("W-10998630")
  public void consistentWithManifest2() {
    MuleExtensionModelDeclarer muleExtensionModelDeclarer = new MuleExtensionModelDeclarer();
    ExtensionDeclarer extensionDeclarer = muleExtensionModelDeclarer.createExtensionModel();
    OperationDeclaration setVariableOperation = extensionDeclarer.getDeclaration().getOperations().stream()
        .filter(operationDeclaration -> operationDeclaration.getName().equals("setVariable")).findFirst().get();
    ParameterDeclaration setVariableParameter = setVariableOperation.getParameterGroup("General").getParameters().stream()
        .filter(parameterDeclaration -> parameterDeclaration.getName().equals("variableName")).findFirst().get();
    ExpressionSupport expressionSupport = setVariableParameter.getExpressionSupport();

    assertThat(expressionSupport.name(), is(expectedResult));
  }
}
