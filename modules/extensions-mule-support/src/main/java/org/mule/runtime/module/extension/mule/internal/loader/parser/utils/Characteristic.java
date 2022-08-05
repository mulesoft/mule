/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.RAISE_ERROR;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkErrorModelParser;

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

  private static final String CHARACTERISTICS_NOT_COMPUTED_MSG = "Characteristics have not been computed yet.";

  private final BiFunction<? super ComponentAst, ? super T, ? extends T> mapper;
  private final T defaultValue;
  private final T stopValue;

  private T value;

  private Characteristic(BiFunction<? super ComponentAst, ? super T, ? extends T> mapper, T defaultValue, T stopValue) {
    this.mapper = mapper;
    this.defaultValue = defaultValue;
    this.stopValue = stopValue;
  }

  /**
   * Use {@param operationAst} to map to the correct value for this Characteristic.
   *
   * @param operationAst is the operation AST. It always has a {@link OperationModel}.
   */
  public void computeFrom(ComponentAst operationAst) {
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

    private BooleanCharacteristic(Predicate<ComponentAst> predicate, Boolean defaultValue, Boolean stopValue) {
      super(((operationAst,
              curValue) -> (curValue != null && Objects.equals(curValue, stopValue)) ? curValue : predicate.test(operationAst)),
            defaultValue, stopValue);
    }

  }

  /**
   * Boolean Characteristic that is meant to finish checking whenever the predicate returns true.
   */
  public static class AnyMatchCharacteristic extends BooleanCharacteristic {

    public AnyMatchCharacteristic(Predicate<ComponentAst> predicate) {
      super(predicate, false, true);
    }

  }

  /**
   * {@link Characteristic} that retrieves all the {@link NotificationModel} emitted by the inner components of this Model
   */
  public static class AggregatedNotificationsCharacteristic extends Characteristic<List<NotificationModel>> {

    public AggregatedNotificationsCharacteristic() {
      super(AggregatedNotificationsCharacteristic::aggregator, emptyList(), null);
    }

    private static List<NotificationModel> aggregator(ComponentAst operationAst, List<NotificationModel> notificationModels) {
      Optional<OperationModel> operationModel = operationAst.getModel(OperationModel.class);
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

    private final Predicate<? super ComponentAst> filterExpression;
    private final Predicate<? super ComponentAst> ignoreExpression;

    private FilteringCharacteristic(BiFunction<? super ComponentAst, T, T> mapper, T defaultValue, T stopValue,
                                    Predicate<? super ComponentAst> filter, Predicate<? super ComponentAst> ignore) {
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

  /**
   * Extension of {@link FilteringCharacteristic} for Boolean features (such as isTransactional)
   */
  public static class BooleanFilteringCharacteristic extends FilteringCharacteristic<Boolean> {

    private BooleanFilteringCharacteristic(Predicate<? super ComponentAst> predicate, Boolean defaultValue, Boolean stopValue,
                                           Predicate<? super ComponentAst> filter, Predicate<? super ComponentAst> ignore) {
      super(((operationAst,
              curValue) -> (curValue != null && Objects.equals(curValue, stopValue)) ? curValue : predicate.test(operationAst)),
            defaultValue, stopValue, filter, ignore);
    }
  }

  /**
   * Boolean Filtered Characteristic that is meant to finish checking whenever the predicate returns true.
   */
  public static class AnyMatchFilteringCharacteristic extends BooleanFilteringCharacteristic {

    public AnyMatchFilteringCharacteristic(Predicate<? super ComponentAst> predicate,
                                           Predicate<? super ComponentAst> filterExpression,
                                           Predicate<? super ComponentAst> ignoreExpression) {
      super(predicate, false, true, filterExpression, ignoreExpression);
    }
  }

  public static class IsBlockingCharacteristic extends AnyMatchCharacteristic {

    public IsBlockingCharacteristic() {
      super(IsBlockingCharacteristic::isBlocking);
    }

    public static boolean isBlocking(ComponentAst operationAst) {
      return operationAst.getModel(OperationModel.class).map(OperationModel::isBlocking).orElse(false).booleanValue();
    }
  }

  public static class IsTransactionalCharacteristic extends AnyMatchFilteringCharacteristic {

    public IsTransactionalCharacteristic() {
      super(IsTransactionalCharacteristic::isTransactional, MuleSdkOperationodelParserUtils::isSkippedScopeForTx,
            MuleSdkOperationodelParserUtils::isIgnoredComponentForTx);
    }

    private static boolean isTransactional(ComponentAst operationAst) {
      return operationAst.getModel(OperationModel.class).map(OperationModel::isTransactional).orElse(false).booleanValue();
    }
  }

  /**
   * {@link Characteristic} that retrieves all the {@link ErrorModelParser} emitted by the inner components of this Model
   */
  public static class AggregatedErrorsCharacteristic extends Characteristic<List<ErrorModelParser>> {

    private static final String ERROR_TYPE_PARAM = "type";

    private static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
        builder().namespace(OPERATION_DSL_NAMESPACE).name(RAISE_ERROR).build();

    public AggregatedErrorsCharacteristic() {
      super(AggregatedErrorsCharacteristic::aggregator, emptyList(), null);
    }

    private static List<ErrorModelParser> aggregator(ComponentAst operationAst, List<ErrorModelParser> errorModels) {
      List<ErrorModelParser> models = errorModels;
      if (models == null) {
        models = new ArrayList<>(5);
      }

      if (isRaiseError(operationAst)) {
        handleRaiseError(operationAst, models);
      } else {
        Optional<OperationModel> operationModel = operationAst.getModel(OperationModel.class);
        if (operationModel.isPresent()) {
          models.addAll(operationModel.get().getErrorModels().stream().map(MuleSdkErrorModelParser::new).collect(toList()));
        }
      }

      return models;
    }

    private static void handleRaiseError(ComponentAst raiseErrorAst, List<ErrorModelParser> errorModels) {
      final ComponentParameterAst typeParameter = raiseErrorAst.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_PARAM);
      if (null == typeParameter) {
        return;
      }

      Optional<String> errorId = typeParameter.getValue().<String>getValue();
      if (!errorId.isPresent()) {
        return;
      }

      // TODO: Use the extension parser's namespace.
      errorModels.add(new MuleSdkErrorModelParser("THIS", errorId.get(), null));
    }

    private static boolean isRaiseError(ComponentAst operationAst) {
      return operationAst.getIdentifier().equals(RAISE_ERROR_IDENTIFIER);
    }
  }
}
