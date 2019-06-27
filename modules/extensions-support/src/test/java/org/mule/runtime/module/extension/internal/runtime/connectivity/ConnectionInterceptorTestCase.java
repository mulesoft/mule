/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.util.Optional.empty;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionInterceptorTestCase extends AbstractMuleContextTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ExecutionContextAdapter operationContext;

  @Mock(lenient = true)
  private ConfigurationInstance configurationInstance;

  @Mock(lenient = true)
  private PetStoreConnector config;

  @Mock(lenient = true)
  private ExtensionConnectionSupplier connectionSupplier;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ConnectionHandler connectionHandler;

  private ConnectionInterceptor interceptor;

  @Before
  public void before() throws Exception {
    when(operationContext.getConfiguration()).thenReturn(Optional.of(configurationInstance));
    when(operationContext.getComponentModel()).thenReturn(operationModel);
    when(operationModel.getModelProperty(PagedOperationModelProperty.class)).thenReturn(empty());
    when(configurationInstance.getValue()).thenReturn(config);

    Map<String, Object> contextVariables = new HashMap<>();

    when(operationContext.getVariable(any())).thenAnswer(
                                                         invocationOnMock -> contextVariables
                                                             .get(invocationOnMock.getArguments()[0]));

    when(operationContext.setVariable(any(), any())).thenAnswer(invocationOnMock -> {
      final Object[] arguments = invocationOnMock.getArguments();
      return contextVariables.put((String) arguments[0], arguments[1]);
    });

    when(operationContext.removeVariable(any()))
        .thenAnswer(invocationOnMock -> contextVariables.remove(invocationOnMock.getArguments()[0]));

    when(operationContext.getTransactionConfig()).thenReturn(empty());

    interceptor = new ConnectionInterceptor();
    setupConnectionSupplier();
  }

  private void setupConnectionSupplier() throws Exception {
    String connectionSupplierKey = "extensions.connection.supplier";

    ((MuleContextWithRegistry) muleContext).getRegistry().unregisterObject(connectionSupplierKey);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(connectionSupplierKey, connectionSupplier);
    muleContext.getInjector().inject(interceptor);

    when(connectionSupplier.getConnection(operationContext)).thenReturn(connectionHandler);
  }

  @Test
  public void pagedOperation() throws Exception {
    when(operationModel.getModelProperty(PagedOperationModelProperty.class))
        .thenReturn(Optional.of(new PagedOperationModelProperty()));

    interceptor.before(operationContext);
    verify(connectionSupplier, never()).getConnection(operationContext);
  }

  @Test
  public void onSuccess() throws Exception {
    interceptor.before(operationContext);
    interceptor.onSuccess(operationContext, null);
    interceptor.after(operationContext, null);

    verify(connectionHandler).release();
  }

  @Test
  public void onConnectionException() throws Exception {
    interceptor.before(operationContext);
    interceptor.onError(operationContext, new ConnectionException("Bleh"));
    interceptor.after(operationContext, null);
    verify(connectionHandler).invalidate();
  }

  @Test
  public void onNonConnectionException() throws Exception {
    interceptor.before(operationContext);
    interceptor.onError(operationContext, new Exception());
    interceptor.after(operationContext, null);
    verify(connectionHandler).release();
  }

  @Test
  public void onNonConnectionExceptionWithSupport() throws Exception {
    when(operationModel.supportsStreaming()).thenReturn(true);
    interceptor.before(operationContext);
    interceptor.onError(operationContext, new Exception());
    interceptor.after(operationContext, null);
    verify(connectionHandler).release();
  }
}
