/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterOrDefault;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interceptor tracks all the {@link Cursor cursors} that were resolved as parameters of a given operation execution and the
 * position they were in <b>before</b> the execution.
 *
 * If the operation fails with a {@link ConnectionException}, then the SDK will attempt to reconnect. This interceptor makes sure
 * that all those cursors are set back on their original positions before the retry is attempted, since otherwise, the stream
 * would be corrupted and the repeatable promise broken.
 *
 * @since 4.1
 */
public class CursorResetInterceptor implements Interceptor<OperationModel> {

  public static final String CURSOR_RESET_HANDLER_VARIABLE = "CURSOR_RESET_HANDLER";
  private final Map<ParameterGroupModel, Set<ParameterModel>> cursorParametersMap;
  private final ReflectionCache reflectionCache;

  public CursorResetInterceptor(Map<ParameterGroupModel, Set<ParameterModel>> cursorParametersMap,
                                ReflectionCache reflectionCache) {
    this.cursorParametersMap = cursorParametersMap;
    this.reflectionCache = reflectionCache;
  }

  @Override
  public void before(ExecutionContext<OperationModel> ctx) throws Exception {
    List<Cursor> cursors = getParameterCursors(ctx);
    if (!cursors.isEmpty()) {
      ((ExecutionContextAdapter<OperationModel>) ctx).setVariable(CURSOR_RESET_HANDLER_VARIABLE,
                                                                  new CursorResetHandler(cursors));
    }
  }

  @Override
  public Throwable onError(ExecutionContext<OperationModel> executionContext, Throwable exception) {
    extractConnectionException(exception).ifPresent(cne -> {
      CursorResetHandler cursorResetHandler =
          ((ExecutionContextAdapter<OperationModel>) executionContext).removeVariable(CURSOR_RESET_HANDLER_VARIABLE);
      if (cursorResetHandler != null) {
        cursorResetHandler.resetCursors();
      }
    });

    return exception;
  }

  private List<Cursor> getParameterCursors(ExecutionContext<OperationModel> ctx) {
    List<Cursor> cursors = new ArrayList<>();
    cursorParametersMap.forEach(((parameterGroupModel, parameterModels) -> {
      parameterModels.forEach(parameterModel -> {
        Object value = getParameterValue(ctx, parameterGroupModel, parameterModel);
        if (value instanceof Cursor) {
          cursors.add((Cursor) value);
        }
      });
    }));
    return cursors;
  }

  private Object getParameterValue(ExecutionContext<OperationModel> ctx, ParameterGroupModel parameterGroupModel,
                                   ParameterModel parameterModel) {
    // TODO MULE-19446: Fix ExecutionContext API to correctly handle parameters value retrieval when defined within a parameter
    // group with showInDsl=true
    Object value = getParameterOrDefault(ctx, parameterGroupModel, parameterModel, null, reflectionCache);
    if (value instanceof TypedValue) {
      value = ((TypedValue) value).getValue();
    }
    return value;
  }
}
