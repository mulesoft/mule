/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PERSONAL_INFORMATION_GROUP_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.KILL_WITH_GROUP;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class LayoutModelTestCase extends AbstractAnnotationsBasedDescriberTestCase {

  private static final String KILL_CUSTOM_OPERATION = "killWithCustomMessage";

  @Before
  public void setUp() {
    setDescriber(describerFor(HeisenbergExtension.class));
  }

  @Test
  public void parseLayoutAnnotationsOnParameter() {
    ExtensionDeclarer declarer = describeExtension();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getParameters();

    assertParameterPlacement(findParameter(parameters, "labeledRicin"), RICIN_GROUP_NAME, 1);
    assertParameterPlacement(findParameter(parameters, "ricinPacks"), RICIN_GROUP_NAME, 2);
  }

  @Test
  public void parseLayoutAnnotationsOnParameterGroup() {
    ExtensionDeclarer declarer = describeExtension();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    List<ParameterDeclaration> parameters = extensionDeclaration.getConfigurations().get(0).getParameters();

    assertParameterPlacement(findParameter(parameters, "myName"), PERSONAL_INFORMATION_GROUP_NAME, 1);
    assertParameterPlacement(findParameter(parameters, "age"), PERSONAL_INFORMATION_GROUP_NAME, 2);
    assertParameterPlacement(findParameter(parameters, "dateOfConception"), PERSONAL_INFORMATION_GROUP_NAME, 3);
    assertParameterPlacement(findParameter(parameters, "dateOfBirth"), PERSONAL_INFORMATION_GROUP_NAME, 4);
    assertParameterPlacement(findParameter(parameters, "dateOfDeath"), PERSONAL_INFORMATION_GROUP_NAME, 5);
  }

  @Test
  public void parseLayoutAnnotationsOnOperationParameter() {
    ExtensionDeclarer declarer = describeExtension();
    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    OperationDeclaration operation = getOperation(extensionDeclaration, KILL_CUSTOM_OPERATION);

    assertThat(operation, is(notNullValue()));
    List<ParameterDeclaration> parameters = operation.getParameters();

    assertParameterPlacement(findParameter(parameters, "victim"), KILL_WITH_GROUP, 1);
    assertParameterPlacement(findParameter(parameters, "goodbyeMessage"), KILL_WITH_GROUP, 2);
  }

  private void assertParameterPlacement(ParameterDeclaration param, String groupName, Integer order) {
    LayoutModel layout = param.getLayoutModel();
    assertThat(layout, is(notNullValue()));

    assertThat(layout.getGroupName(), equalTo(ofNullable(groupName)));
    assertThat(layout.getOrder(), equalTo(ofNullable(order)));
  }
}
