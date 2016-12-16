/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.tx.OperationTransactionalAction.JOIN_IF_POSSIBLE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_TAB_NAME;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TransactionalModelEnricherTestCase extends AbstractMuleTestCase {

  private static final String TRANSACTIONAL_OPERATION = "transactionalOperation";
  private static final String NOT_TRANSACTIONAL_OPERATION = "notConnectedOperation";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private DescribingContext describingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  private OperationDeclaration transactionalOperation;

  private OperationDeclaration notTransactionalOperation;

  private ModelEnricher enricher = new TransactionalModelEnricher();

  private MetadataType transactionalActionType;

  @Before
  public void before() throws Exception {
    transactionalActionType =
        ExtensionsTypeLoaderFactory.getDefault().createTypeLoader().load(OperationTransactionalAction.class);
    transactionalOperation = spy(new ExtensionDeclarer()
        .withOperation(TRANSACTIONAL_OPERATION)
        .transactional(true)
        .getDeclaration());

    notTransactionalOperation = spy(new ExtensionDeclarer()
        .withOperation(NOT_TRANSACTIONAL_OPERATION)
        .transactional(false)
        .getDeclaration());

    when(describingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(asList(transactionalOperation, notTransactionalOperation));
  }

  @Test
  public void enrich() throws Exception {
    enricher.enrich(describingContext);
    ParameterDeclaration transactionParameter = getTransactionActionParameter(transactionalOperation).orElse(null);
    assertThat(transactionParameter, is(notNullValue()));

    assertThat(transactionParameter.getType(), equalTo(transactionalActionType));
    assertThat(transactionParameter.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(transactionParameter.isRequired(), is(false));
    assertThat(transactionParameter.getDefaultValue(), is(JOIN_IF_POSSIBLE));
    assertThat(transactionParameter.getDescription(), is(TRANSACTIONAL_ACTION_PARAMETER_DESCRIPTION));
    assertThat(transactionParameter.getLayoutModel().getTabName().get(), is(TRANSACTIONAL_TAB_NAME));
  }

  @Test
  public void enrichOnlyOnceWhenFlyweight() throws Exception {
    when(extensionDeclaration.getOperations())
        .thenReturn(asList(transactionalOperation, transactionalOperation, notTransactionalOperation));
    enricher.enrich(describingContext);
    assertThat(getTransactionActionParameter(transactionalOperation).isPresent(), is(true));
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void badTransactionalActionParameter() {
    ParameterDeclaration offending = mock(ParameterDeclaration.class);
    when(offending.getName()).thenReturn(TRANSACTIONAL_ACTION_PARAMETER_NAME);
    when(transactionalOperation.getAllParameters()).thenReturn(singletonList(offending));

    enricher.enrich(describingContext);
  }

  private Optional<ParameterDeclaration> getTransactionActionParameter(OperationDeclaration declaration) {
    List<ParameterDeclaration> txParameters = declaration.getParameterGroup(DEFAULT_GROUP_NAME).getParameters().stream()
        .filter(p -> p.getName().equals(TRANSACTIONAL_ACTION_PARAMETER_NAME))
        .collect(toList());

    assertThat(txParameters, anyOf(hasSize(1), hasSize(0)));

    return txParameters.isEmpty() ? empty() : of(txParameters.get(0));
  }
}
