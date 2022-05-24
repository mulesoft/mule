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

/**
 * Utilitarian class to generalize parsing and calculation of different features of Mule Operations.
 * 
 * @since 4.5
 */
public class Characteristic<T> {

  private static final String CHARACTERISTICS_NOT_COMPUTED_MSG = "Characteristics have not been computed yet.";

  private final BiFunction<OperationModel, T, T> mapper;
  private final T defaultValue;
  private final T stopValue;

  private T value;

  private Characteristic(BiFunction<OperationModel, T, T> mapper, T defaultValue, T stopValue) {
    this.mapper = mapper;
    this.defaultValue = defaultValue;
    this.stopValue = stopValue;
  }

  /**
   * Use {@param operationModel} to map to the correct value for this Characteristic
   */
  public void computeFrom(OperationModel operationModel) {
    value = mapper.apply(operationModel, value);
  }

  /**
   * Use default value as value for this Characteristic
   */
  public void setWithDefault() {
    value = defaultValue;
  }

  /**
   * @return if there is a definitive (final) value for this Characteristic
   */
  public boolean hasDefinitiveValue() {
    if (stopValue == null) {
      return false;
    }
    return stopValue.equals(value);
  }

  /**
   * @return if this Characteristic has already a value calculated
   */
  public boolean hasValue() {
    return value != null;
  }

  /**
   * @return the defined value (either calculated, or if using default value)
   */
  public T getValue() {
    checkState(hasValue(), CHARACTERISTICS_NOT_COMPUTED_MSG);
    return value;
  }

  /**
   * Extension of {@link Characteristic} for Boolean features (such as isBlocking)
   */
  public static class BooleanCharacteristic extends Characteristic<Boolean> {

    private BooleanCharacteristic(Predicate<OperationModel> predicate, Boolean defaultValue, Boolean stopValue) {
      super(((operationModel,
              curValue) -> (curValue != null && curValue == stopValue) ? curValue : predicate.test(operationModel)),
            defaultValue, stopValue);
    }

  }

  public static class AnyMatchCharacteristic extends BooleanCharacteristic {

    public AnyMatchCharacteristic(Predicate<OperationModel> predicate) {
      super(predicate, false, true);
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

  /**
   * Extension of {@link Characteristic} that has also criteria for ignoring and skipping whole components (e.g. for
   * isTransactional)
   */
  public static class FilteringCharacteristic<T> extends Characteristic<T> {

    private final Predicate<ComponentAst> filterExpression;
    private final Predicate<ComponentAst> ignoreExpression;

    private FilteringCharacteristic(BiFunction<OperationModel, T, T> mapper, T defaultValue, T stopValue,
                                    Predicate<ComponentAst> filter, Predicate<ComponentAst> ignore) {
      super(mapper, defaultValue, stopValue);
      this.filterExpression = filter;
      this.ignoreExpression = ignore;
    }

    public boolean filterComponent(ComponentAst componentAst) {
      return filterExpression.test(componentAst);
    }

    public boolean ignoreComponent(ComponentAst componentAst) {
      return ignoreExpression.test(componentAst);
    }
  }

  public static class BooleanFilteringCharacteristic extends FilteringCharacteristic<Boolean> {

    private BooleanFilteringCharacteristic(Predicate<OperationModel> predicate, Boolean defaultValue, Boolean stopValue,
                                           Predicate<ComponentAst> filter, Predicate<ComponentAst> ignore) {
      super(((operationModel,
              curValue) -> (curValue != null && curValue == stopValue) ? curValue : predicate.test(operationModel)),
            defaultValue, stopValue, filter, ignore);
    }
  }

  public static class AnyMatchFilteringCharacteristic extends BooleanFilteringCharacteristic {

    public AnyMatchFilteringCharacteristic(Predicate<OperationModel> predicate, Predicate<ComponentAst> filterExpression,
                                           Predicate<ComponentAst> ignoreExpression) {
      super(predicate, false, true, filterExpression, ignoreExpression);
    }
  }
}
