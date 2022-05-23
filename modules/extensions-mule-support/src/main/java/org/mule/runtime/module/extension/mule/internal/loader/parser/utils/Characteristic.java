/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkState;

public class Characteristic<T> {

  private static final String CHARACTERISTICS_NOT_COMPUTED_MSG = "Characteristics have not been computed yet.";

  private final BiFunction<OperationModel, T, T> mapper;
  private final T defaultValue;
  private final T stopValue;
  private final Predicate<ComponentAst> filterCondition;

  private T value;

  private Characteristic(BiFunction<OperationModel, T, T> mapper, T defaultValue, T stopValue) {
    this(mapper, defaultValue, stopValue, ast -> false);
  }

  public Characteristic(BiFunction<OperationModel, T, T> mapper, T defaultValue, T stopValue, Predicate<ComponentAst> filter) {
    this.mapper = mapper;
    this.defaultValue = defaultValue;
    this.stopValue = stopValue;
    this.filterCondition = filter;
  }

  public void computeFrom(OperationModel operationModel) {
    value = mapper.apply(operationModel, value);
  }

  public void setWithDefault() {
    value = defaultValue;
  }

  public boolean hasDefinitiveValue() {
    if (stopValue == null) {
      return false;
    }
    return stopValue.equals(value);
  }

  public boolean hasValue() {
    return value != null;
  }

  public T getValue() {
    checkState(hasValue(), CHARACTERISTICS_NOT_COMPUTED_MSG);
    return value;
  }

  public boolean filterComponent(ComponentAst componentAst) {
    return this.filterCondition.test(componentAst);
  }

  public static class BooleanCharacteristic extends Characteristic<Boolean> {

    private BooleanCharacteristic(Predicate<OperationModel> predicate, Boolean defaultValue, Boolean stopValue) {
      super(((operationModel,
              curValue) -> (curValue != null && curValue == stopValue) ? curValue : predicate.test(operationModel)),
            defaultValue, stopValue);
    }

    private BooleanCharacteristic(Predicate<OperationModel> predicate, Boolean defaultValue, Boolean stopValue,
                                  Predicate<ComponentAst> filterCondition) {
      super(((operationModel,
              curValue) -> (curValue != null && curValue == stopValue) ? curValue : predicate.test(operationModel)),
            defaultValue, stopValue, filterCondition);
    }
  }

  public static class AnyMatchCharacteristic extends BooleanCharacteristic {

    public AnyMatchCharacteristic(Predicate<OperationModel> predicate) {
      super(predicate, false, true);
    }

    public AnyMatchCharacteristic(Predicate<OperationModel> predicate, Predicate<ComponentAst> filterCondition) {
      super(predicate, false, true, filterCondition);
    }
  }

  public static class AggregatedNotificationsCharacteristic extends Characteristic<List<NotificationModel>> {

    public AggregatedNotificationsCharacteristic() {
      super(AggregatedNotificationsCharacteristic::aggregator, emptyList(), null);
    }

    private static List<NotificationModel> aggregator(OperationModel operationModel, List<NotificationModel> notificationModels) {
      if (notificationModels == null) {
        notificationModels = new ArrayList<>();
      }
      notificationModels.addAll(operationModel.getNotificationModels());
      return notificationModels;
    }
  }
}
