/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.assertLayoutModel;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ParameterGroupLayoutOrderTestCase {

  private static final String GROUP_NAME = "Group1";

  private ParameterizedModel parameterizedModel;
  private ExtensionModel extensionModel;

  @Before
  public void setUp() {
    extensionModel = loadExtension(OrderedExtension.class);
  }

  @Test
  public void configGroupImplicitOrderTestCase() {
    parameterizedModel = getConfig("GroupImplicitOrderConfig");
    assertGroupOrder(parameterizedModel, 1, 2);
  }

  @Test
  public void configGroupExplicitOrderTestCase() {
    parameterizedModel = getConfig("GroupExplicitOrderConfig");
    assertGroupOrder(parameterizedModel, 1, 2);
  }

  @Test
  public void configGroupReorderGeneralTestCase() {
    parameterizedModel = getConfig("GroupReorderGeneralConfig");
    assertGroupOrder(parameterizedModel, 0, 1);
  }

  @Test
  public void configGroupNoGeneralTestCase() {
    parameterizedModel = getConfig("GroupNoGeneralConfig");
    assertParameterOrder(parameterizedModel, GROUP_NAME, 1);
  }


  @Test
  public void configGroupOnlyGeneralTestCase() {
    parameterizedModel = getConfig("GroupOnlyGeneralConfig");
    assertParameterOrder(parameterizedModel, DEFAULT_GROUP_NAME, 1);
  }

  @Test
  public void operationGroupImplicitOrderTestCase() {
    parameterizedModel = getOperation("implicitOrder");
    assertGroupOrder(parameterizedModel, 1, 2);
  }

  @Test
  public void operationGroupExplicitOrderTestCase() {
    parameterizedModel = getOperation("explicitOrder");
    assertGroupOrder(parameterizedModel, 1, 2);
  }

  @Test
  public void operationGroupReorderGeneralTestCase() {
    parameterizedModel = getOperation("reorder");
    assertGroupOrder(parameterizedModel, 0, 1);
  }

  @Test
  public void operationGroupNoGeneralTestCase() {
    parameterizedModel = getOperation("noGeneral");
    assertParameterOrder(parameterizedModel, GROUP_NAME, 1);
  }

  @Test
  public void operationGroupOnlyGeneralTestCase() {
    parameterizedModel = getOperation("onlyGeneral");
    assertParameterOrder(parameterizedModel, DEFAULT_GROUP_NAME, 1);
  }

  private ConfigurationModel getConfig(String name) {
    return extensionModel.getConfigurationModel(name).get();
  }

  private OperationModel getOperation(String name) {
    return extensionModel.getOperationModel(name).get();
  }

  private void assertGroupOrder(ParameterizedModel parameterizedModel, int expectedOrder, int expectedOrder2) {
    assertParameterOrder(parameterizedModel, DEFAULT_GROUP_NAME, expectedOrder);
    assertParameterOrder(parameterizedModel, GROUP_NAME, expectedOrder2);
  }

  private void assertParameterOrder(ParameterizedModel parameterizedModel, String parameterName, int expectedOrder) {
    ParameterGroupModel paramOne = getNamedObject(parameterizedModel.getParameterGroupModels(), parameterName);
    Optional<LayoutModel> layoutModel = paramOne.getLayoutModel();
    assertLayoutModel(parameterName, expectedOrder, layoutModel);
  }

  @Extension(name = "OrderedExtension")
  @Operations({OrderedOperations.class})
  @Configurations({ParameterGroupImplicitOrder.class, ParameterGroupExplicitOrder.class, ParameterGroupReorder.class,
      ParameterGroupNoGeneral.class, ParameterGroupOnlyGeneral.class})
  public static class OrderedExtension {

  }

  public static class GroupImplicitOrder {

    @Parameter
    String paramOne;

    @Parameter
    String paramThree;
  }

  @Configuration(name = "GroupImplicitOrderConfig")
  public static class ParameterGroupImplicitOrder {

    @ParameterGroup(name = GROUP_NAME)
    GroupImplicitOrder groupOne;

    @Parameter
    String paramOneGeneral;

    @Parameter
    String paramTwoGeneral;

  }

  @Configuration(name = "GroupExplicitOrderConfig")
  public static class ParameterGroupExplicitOrder {

    @Placement(order = 2)
    @ParameterGroup(name = GROUP_NAME)
    GroupImplicitOrder groupOne;

    @Parameter
    String paramOneGeneral;

    @Parameter
    String paramTwoGeneral;

  }

  @Configuration(name = "GroupReorderGeneralConfig")
  public static class ParameterGroupReorder {

    @Placement(order = 1)
    @ParameterGroup(name = GROUP_NAME)
    GroupImplicitOrder groupOne;

    @Parameter
    String paramOneGeneral;

    @Parameter
    String paramTwoGeneral;

  }

  @Configuration(name = "GroupNoGeneralConfig")
  public static class ParameterGroupNoGeneral {

    @Placement(order = 1)
    @ParameterGroup(name = GROUP_NAME)
    GroupImplicitOrder groupOne;
  }

  @Configuration(name = "GroupOnlyGeneralConfig")
  public static class ParameterGroupOnlyGeneral {

    @Parameter
    String paramOneGeneral;

    @Parameter
    String paramTwoGeneral;
  }

  public static class OrderedOperations {

    public void noGeneral(@Placement(order = 1) @ParameterGroup(name = GROUP_NAME) GroupImplicitOrder group) {}

    public void reorder(String param, @Placement(order = 1) @ParameterGroup(name = GROUP_NAME) GroupImplicitOrder group) {}

    public void explicitOrder(String param, @Placement(order = 2) @ParameterGroup(name = GROUP_NAME) GroupImplicitOrder group) {}

    public void implicitOrder(String param, @ParameterGroup(name = GROUP_NAME) GroupImplicitOrder group) {}

    public void onlyGeneral(String param) {}
  }
}
