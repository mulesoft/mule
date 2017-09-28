/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;


import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.assertLayoutModel;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
public class ParameterLayoutOrderDeclarationEnricherTestCase {

  @Parameterized.Parameter(0)
  public ParameterizedModel parameterizedModel;

  @Parameterized.Parameter(1)
  public String name;

  @Parameterized.Parameters(name = "{1}")
  public static List<Object[]> parameters() {
    ExtensionModel extensionModel = loadExtension(OrderedExtension.class);

    List<Object[]> objects = new ArrayList<>();
    objects.add(new Object[] {extensionModel.getOperationModel("implicitOrder").get(), "Operation - Implicit Order"});
    objects.add(new Object[] {extensionModel.getOperationModel("explicitOrder").get(), "Operation - Explicit Order"});
    objects.add(new Object[] {extensionModel.getOperationModel("mixedOrder").get(), "Operation - Mixed Order"});
    objects.add(new Object[] {extensionModel.getConfigurationModel("ImplicitOrderConfig").get(), "Config - Implicit Order"});
    objects.add(new Object[] {extensionModel.getConfigurationModel("ExplicitOrderConfig").get(), "Config - Explicit Order"});
    objects.add(new Object[] {extensionModel.getConfigurationModel("MixedOrderConfig").get(), "Config - Mixed Order"});
    objects.add(new Object[] {extensionModel.getConfigurationModel("InheritsOrderConfig").get(), "Config - Inherited Order"});
    objects.add(new Object[] {extensionModel.getConnectionProviderModel("implicit").get(), "Conn Provider - Implicit Order"});
    objects.add(new Object[] {extensionModel.getConnectionProviderModel("explicit").get(), "Conn Provider - Explicit Order"});
    objects.add(new Object[] {extensionModel.getConnectionProviderModel("mixed").get(), "Conn Provider - Mixed Order"});
    objects.add(new Object[] {extensionModel.getSourceModel("ImplicitSourceOrder").get(), "Source - Implicit Order"});
    objects.add(new Object[] {extensionModel.getSourceModel("ExplicitSourceOrder").get(), "Source - Explicit Order"});
    objects.add(new Object[] {extensionModel.getSourceModel("MixedSourceOrder").get(), "Source - Mixed Order"});
    objects.add(new Object[] {extensionModel.getSourceModel("ExplicitSourceOrderWithCallbacks").get(),
        "Source - With Callbacks Explicit Order"});
    objects.add(new Object[] {extensionModel.getSourceModel("MixedSourceOrderWithCallbacks").get(),
        "Source - With Callbacks Mixed Order"});

    return objects;
  }

  @Test
  public void assertParametersOrder() {
    assertParameterOrder(parameterizedModel, "paramOne", 1);
    assertParameterOrder(parameterizedModel, "paramTwo", 2);
    assertParameterOrder(parameterizedModel, "paramThree", 3);
  }

  private void assertParameterOrder(ParameterizedModel parameterizedModel, String parameterName, int expectedOrder) {
    ParameterModel paramOne = getNamedObject(parameterizedModel.getAllParameterModels(), parameterName);
    assertLayoutModel(parameterName, expectedOrder, paramOne.getLayoutModel());
  }

  @Operations(OrderedOperations.class)
  @Extension(name = "OrderedExtension")
  @Configurations({ImplicitOrderConfig.class, ExplicitOrderConfig.class, MixedOrderConfig.class, InheritsOrderConfig.class})
  @ConnectionProviders({ImplicitConnProvider.class, ExplicitConnProvider.class, MixedConnProvider.class})
  @Sources({ImplicitSourceOrder.class, ExplicitSourceOrder.class, MixedSourceOrder.class, ExplicitSourceOrderWithCallbacks.class,
      MixedSourceOrderWithCallbacks.class})
  public static class OrderedExtension {

  }

  public static class OrderedOperations {

    public void implicitOrder(String paramOne, String paramTwo, String paramThree) {

    }

    public void explicitOrder(@Placement(order = 1) String paramOne, @Placement(order = 2) String paramTwo,
                              @Placement(order = 3) String paramThree) {

    }

    public void mixedOrder(String paramTwo, @Placement(order = 1) String paramOne, String paramThree) {

    }
  }

  @Configuration(name = "ImplicitOrderConfig")
  public static class ImplicitOrderConfig {

    @Parameter
    String paramOne;

    @Parameter
    String paramTwo;

    @Parameter
    String paramThree;

  }

  @Configuration(name = "ExplicitOrderConfig")
  public static class ExplicitOrderConfig {

    @Parameter
    @Placement(order = 1)
    String paramOne;

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    @Placement(order = 3)
    String paramThree;

  }

  @Configuration(name = "MixedOrderConfig")
  public static class MixedOrderConfig {

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    String paramOne;

    @Parameter
    String paramThree;
  }

  @Configuration(name = "InheritsOrderConfig")
  public static class InheritsOrderConfig extends AbstractConfig {

    @Parameter
    String paramOne;

    @Parameter
    String paramThree;
  }

  private static abstract class AbstractConfig {

    @Parameter
    @Placement(order = 2)
    String paramTwo;
  }

  @Alias("implicit")
  public static class ImplicitConnProvider implements ConnectionProvider<Object> {

    @Parameter
    String paramOne;

    @Parameter
    String paramTwo;

    @Parameter
    String paramThree;

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }

  @Alias("explicit")
  public static class ExplicitConnProvider implements ConnectionProvider<Object> {

    @Parameter
    @Placement(order = 1)
    String paramOne;

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    @Placement(order = 3)
    String paramThree;

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }

  @Alias("mixed")
  public static class MixedConnProvider implements ConnectionProvider<Object> {

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    String paramOne;

    @Parameter
    String paramThree;

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }


  @MediaType(value = ANY, strict = false)
  public static class ImplicitSourceOrder extends Source<String, Object> {

    @Parameter
    String paramOne;

    @Parameter
    String paramTwo;

    @Parameter
    String paramThree;

    @Override
    public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @MediaType(TEXT_PLAIN)
  public static class ExplicitSourceOrder extends Source<String, Object> {

    @Parameter
    @Placement(order = 1)
    String paramOne;

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    @Placement(order = 3)
    String paramThree;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @MediaType(value = ANY, strict = false)
  public static class MixedSourceOrder extends Source<String, Object> {

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Parameter
    String paramOne;

    @Parameter
    String paramThree;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @MediaType(TEXT_PLAIN)
  public static class ExplicitSourceOrderWithCallbacks extends Source<String, Object> {

    @Parameter
    @Placement(order = 2)
    String paramTwo;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }

    @OnSuccess
    public void onSuccess(@Placement(order = 1) String paramOne) {

    }

    @OnError
    public void onError(@Placement(order = 3) String paramThree) {

    }

  }
  public static class MixedSourceOrderWithCallbacks extends Source<Apple, Object> {

    @Parameter
    @Placement(order = 1)
    String paramOne;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }

    @OnSuccess
    public void onSuccess(@Placement(order = 3) String paramThree) {

    }

    @OnError
    public void onError(@Placement(order = 2) String paramTwo) {

    }
  }

}
