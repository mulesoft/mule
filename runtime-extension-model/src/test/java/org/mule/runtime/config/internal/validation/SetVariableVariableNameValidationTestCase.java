package org.mule.runtime.config.internal.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@Issue("W-10998630")
public class SetVariableVariableNameValidationTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new NoExpressionsInNoExpressionsSupportedParams();
  }

  @Test
  public void invalidParameterWithExpression() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\"\n"
        +
        "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "   xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
        +
        "   <flow name=\"expression-for-variablenameFlow\">\n" +
        "       <set-variable value=\"myVariable\" variableName=\"targetName\"/>" +
        "       <set-variable value=\"specialValue\" variableName=\"#[vars.targetName]\"/>\n" +
        "   </flow>\n" +
        "</mule>")
            .stream().findFirst();

    assertThat(msg.isPresent(), is(false));
  }
}
