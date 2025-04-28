/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static org.mule.runtime.api.util.Preconditions.checkState;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Utilitarian class to generalize parsing and calculation of different features of Mule Operations.
 *
 * @since 4.5
 */
public class Characteristic<T> {

  public static class ComponentAstWithHierarchy {

    private final ComponentAst componentAst;
    private final List<ComponentAst> hierarchy;

    public ComponentAstWithHierarchy(Pair<ComponentAst, List<ComponentAst>> pair) {
      this(pair.getFirst(), pair.getSecond());
    }

    public ComponentAstWithHierarchy(ComponentAst componentAst, List<ComponentAst> hierarchy) {
      this.componentAst = componentAst;
      this.hierarchy = hierarchy;
    }

    public ComponentAst getComponentAst() {
      return componentAst;
    }

    public List<ComponentAst> getHierarchy() {
      return hierarchy;
    }

    @Override
    public String toString() {
      return "ComponentAstWithHierarchy{" +
          "componentAst=" + componentAst +
          ", hierarchy=" + hierarchy +
          '}';
    }
  }

  private static final String CHARACTERISTICS_NOT_COMPUTED_MSG = "Characteristics have not been computed yet.";

  private final BiFunction<ComponentAstWithHierarchy, ? super T, ? extends T> mapper;
  private final T defaultValue;
  private final T stopValue;

  private T value;

  protected Characteristic(BiFunction<ComponentAstWithHierarchy, ? super T, ? extends T> mapper, T defaultValue, T stopValue) {
    this.mapper = mapper;
    this.defaultValue = defaultValue;
    this.stopValue = stopValue;
  }

  /**
   * Use {@param operationAst} to map to the correct value for this Characteristic.
   *
   * @param operationAst is the operation AST. It always has a {@link OperationModel}.
   */
  public void computeFrom(ComponentAstWithHierarchy operationAst) {
    value = mapper.apply(operationAst, value);
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

    private BooleanCharacteristic(Predicate<ComponentAstWithHierarchy> predicate, Boolean defaultValue, Boolean stopValue) {
      super(((operationAst,
              curValue) -> (curValue != null && Objects.equals(curValue, stopValue)) ? curValue : predicate.test(operationAst)),
            defaultValue, stopValue);
    }

  }

  /**
   * Boolean Characteristic that is meant to finish checking whenever the predicate returns true.
   */
  public static class AnyMatchCharacteristic extends BooleanCharacteristic {

    public AnyMatchCharacteristic(Predicate<ComponentAstWithHierarchy> predicate) {
      super(predicate, false, true);
    }

    public AnyMatchCharacteristic(Predicate<OperationModel> predicate, boolean defaultIfOperationModelNotPresent) {
      this(fromOperationModelPredicate(predicate, defaultIfOperationModelNotPresent));
    }
  }

  /**
   * {@link Characteristic} that retrieves all the {@link NotificationModel} emitted by the inner components of this Model
   */
  public static class AggregatedNotificationsCharacteristic extends Characteristic<List<NotificationModel>> {

    public AggregatedNotificationsCharacteristic() {
      super(AggregatedNotificationsCharacteristic::aggregator, emptyList(), null);
    }

    private static List<NotificationModel> aggregator(ComponentAstWithHierarchy operationAst,
                                                      List<NotificationModel> notificationModels) {
      Optional<OperationModel> operationModel = operationAst.getComponentAst().getModel(OperationModel.class);
      if (notificationModels == null) {
        notificationModels = new ArrayList<>();
      }
      notificationModels.addAll(operationModel.map(OperationModel::getNotificationModels).orElse(emptySet()));
      return notificationModels;
    }
  }

  /**
   * Extension of {@link Characteristic} that has also criteria for ignoring and skipping whole components (e.g. for
   * isTransactional)
   */
  public static class FilteringCharacteristic<T> extends Characteristic<T> {

    private final Predicate<? super ComponentAstWithHierarchy> filterExpression;
    private final Predicate<? super ComponentAstWithHierarchy> ignoreExpression;

    private FilteringCharacteristic(BiFunction<ComponentAstWithHierarchy, T, T> mapper, T defaultValue, T stopValue,
                                    Predicate<? super ComponentAstWithHierarchy> filter,
                                    Predicate<? super ComponentAstWithHierarchy> ignore) {
      super(mapper, defaultValue, stopValue);
      this.filterExpression = filter;
      this.ignoreExpression = ignore;
    }

    public boolean filterComponent(ComponentAstWithHierarchy componentAst) {
      return filterExpression.test(componentAst);
    }

    public boolean ignoreComponent(ComponentAstWithHierarchy componentAst) {
      return ignoreExpression.test(componentAst);
    }
  }

  /**
   * Extension of {@link FilteringCharacteristic} for Boolean features (such as isTransactional)
   */
  public static class BooleanFilteringCharacteristic extends FilteringCharacteristic<Boolean> {

    private BooleanFilteringCharacteristic(Predicate<ComponentAstWithHierarchy> predicate, Boolean defaultValue,
                                           Boolean stopValue,
                                           Predicate<ComponentAstWithHierarchy> filter,
                                           Predicate<ComponentAstWithHierarchy> ignore) {
      super(((operationAst,
              curValue) -> (curValue != null && Objects.equals(curValue, stopValue)) ? curValue : predicate.test(operationAst)),
            defaultValue, stopValue, filter, ignore);
    }
  }

  /**
   * Boolean Filtered Characteristic that is meant to finish checking whenever the predicate returns true.
   */
  public static class AnyMatchFilteringCharacteristic extends BooleanFilteringCharacteristic {

    public AnyMatchFilteringCharacteristic(Predicate<ComponentAstWithHierarchy> predicate,
                                           Predicate<ComponentAstWithHierarchy> filterExpression,
                                           Predicate<ComponentAstWithHierarchy> ignoreExpression) {
      super(predicate, false, true, filterExpression, ignoreExpression);
    }
  }

  public static class IsBlockingCharacteristic extends AnyMatchCharacteristic {

    public IsBlockingCharacteristic() {
      super(OperationModel::isBlocking, false);
    }
  }

  public static class IsConnectedCharacteristic extends AnyMatchCharacteristic {

    public IsConnectedCharacteristic() {
      super(OperationModel::requiresConnection, false);
    }
  }

  public static class IsTransactionalCharacteristic extends AnyMatchFilteringCharacteristic {

    public IsTransactionalCharacteristic() {
      super(fromOperationModelPredicate(OperationModel::isTransactional, false),
            MuleSdkOperationModelParserUtils::isSkippedScopeForTx,
            MuleSdkOperationModelParserUtils::isIgnoredComponentForTx);
    }
  }

  private static Predicate<ComponentAstWithHierarchy> fromOperationModelPredicate(Predicate<OperationModel> operationModelPredicate,
                                                                                  boolean defaultIfOperationModelNotPresent) {
    return componentAstWithHierarchy -> componentAstWithHierarchy.getComponentAst()
        .getModel(OperationModel.class)
        .map(operationModelPredicate::test)
        .orElse(defaultIfOperationModelNotPresent);
  }
}
