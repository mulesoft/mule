/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.utils;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AggregatedNotificationsCharacteristic;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AnyMatchCharacteristic;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AnyMatchFilteringCharacteristic;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.FilteringCharacteristic;
import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.model.notification.ImmutableNotificationModel;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;


@Feature(REUSE)
@Story(OPERATIONS)
public class CharacteristicsTestsCase {

  @Test
  @Description("Checks that AnyMatchCharacteristic is updated accordingly after different computations")
  public void anyMatchTestCase() {
    ComponentAst operationAst = createOperationAst();
    Reference<Boolean> value = new Reference<>(FALSE);
    Characteristic<Boolean> characteristic = new AnyMatchCharacteristic(x -> value.get());
    assertThat(characteristic.hasValue(), is(false));
    assertThat(characteristic.hasDefinitiveValue(), is(false));

    characteristic.computeFrom(operationAst);
    assertThat(characteristic.hasValue(), is(true));
    assertThat(characteristic.hasDefinitiveValue(), is(false));
    assertThat(characteristic.getValue(), is(false));

    value.set(true);
    characteristic.computeFrom(operationAst);
    assertThat(characteristic.hasValue(), is(true));
    assertThat(characteristic.hasDefinitiveValue(), is(true));
    assertThat(characteristic.getValue(), is(true));
  }

  private ComponentAst createOperationAst() {
    OperationModel operationModel = mock(OperationModel.class);
    ComponentAst operationAst = mock(ComponentAst.class);
    when(operationAst.getModel(OperationModel.class)).thenReturn(ofNullable(operationModel));
    return operationAst;
  }

  @Test
  @Description("Checks that AnyMatchFilteringCharacteristic is updated accordingly after different computations, including filters")
  public void anyMathFilterTestCase() {
    ComponentAst operationAst = createOperationAst();
    ComponentAst ast = mock(ComponentAst.class);
    Reference<Boolean> value = new Reference<>(FALSE);
    Reference<Boolean> filter = new Reference<>(FALSE);
    Reference<Boolean> ignore = new Reference<>(FALSE);
    FilteringCharacteristic<Boolean> characteristic =
        new AnyMatchFilteringCharacteristic(x -> value.get(), x -> filter.get(), x -> ignore.get());
    assertThat(characteristic.hasValue(), is(false));
    assertThat(characteristic.hasDefinitiveValue(), is(false));

    characteristic.computeFrom(operationAst);
    assertThat(characteristic.hasValue(), is(true));
    assertThat(characteristic.hasDefinitiveValue(), is(false));
    assertThat(characteristic.getValue(), is(false));

    value.set(true);
    characteristic.computeFrom(operationAst);
    assertThat(characteristic.hasValue(), is(true));
    assertThat(characteristic.hasDefinitiveValue(), is(true));
    assertThat(characteristic.getValue(), is(true));

    assertThat(characteristic.filterComponent(ast), is(false));
    assertThat(characteristic.ignoreComponent(ast), is(false));

    filter.set(true);
    assertThat(characteristic.filterComponent(ast), is(true));
    assertThat(characteristic.ignoreComponent(ast), is(false));

    ignore.set(true);
    assertThat(characteristic.filterComponent(ast), is(true));
    assertThat(characteristic.ignoreComponent(ast), is(true));
  }

  @Test
  @Description("Checks that AggregatedNotificationsCharacteristic is updated accordingly and aggregates results after different computations")
  public void aggregatedTestCase() {
    ComponentAst componentAst1 = createOperationAst();
    ComponentAst componentAst2 = createOperationAst();
    ComponentAst componentAst3 = createOperationAst();
    NotificationModel notification1 = new ImmutableNotificationModel("test", "1", mock(MetadataType.class));
    NotificationModel notification2 = new ImmutableNotificationModel("test", "2", mock(MetadataType.class));
    NotificationModel notification3 = new ImmutableNotificationModel("test", "3", mock(MetadataType.class));
    NotificationModel notification4 = new ImmutableNotificationModel("test", "4", mock(MetadataType.class));
    NotificationModel notification5 = new ImmutableNotificationModel("test", "5", mock(MetadataType.class));
    when(componentAst1.getModel(OperationModel.class).get().getNotificationModels())
        .thenReturn(new HashSet<>(asList(notification1, notification2)));
    when(componentAst2.getModel(OperationModel.class).get().getNotificationModels())
        .thenReturn(new HashSet<>(asList(notification3, notification4)));
    when(componentAst3.getModel(OperationModel.class).get().getNotificationModels())
        .thenReturn(new HashSet<>(singletonList(notification5)));

    Characteristic<List<NotificationModel>> characteristic = new AggregatedNotificationsCharacteristic();

    assertThat(characteristic.hasDefinitiveValue(), is(false));

    characteristic.computeFrom(componentAst1);
    characteristic.computeFrom(componentAst2);
    characteristic.computeFrom(componentAst3);
    assertThat(characteristic.hasDefinitiveValue(), is(false));
    assertThat(characteristic.hasValue(), is(true));
    assertThat(characteristic.getValue(), hasSize(5));
    assertThat(characteristic.getValue(), hasItems(notification1, notification2, notification3, notification4, notification5));
  }

}
