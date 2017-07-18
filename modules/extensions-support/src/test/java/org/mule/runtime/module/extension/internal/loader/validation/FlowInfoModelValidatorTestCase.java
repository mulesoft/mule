/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.runtime.FlowInfo;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FlowInfoModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceModel sourceModel;

  private FlowInfoModelValidator validator = new FlowInfoModelValidator();
  private ProblemsReporter reporter = new ProblemsReporter(extensionModel);

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn("test");
    when(extensionModel.getSourceModels()).thenReturn(singletonList(sourceModel));
    when(sourceModel.getSuccessCallback()).thenReturn(empty());
    when(sourceModel.getErrorCallback()).thenReturn(empty());
  }

  @Test
  public void noImplementingType() {
    when(sourceModel.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(empty());
    assertValid();
  }

  @Test
  public void noFlowInfoField() {
    mockSourceType(NoFlowInfoSource.class);
    assertValid();
  }

  @Test
  public void oneFlowInfoField() {
    mockSourceType(OneFlowInfoSource.class);
    assertValid();
  }

  @Test
  public void twoFlowInfoField() {
    mockSourceType(TwoFlowInfoSource.class);
    validator.validate(extensionModel, reporter);
    assertThat(reporter.getErrors(), hasSize(1));
    assertThat(reporter.getErrors().get(0).getMessage(), allOf(
                                                               containsString(FlowInfo.class.getSimpleName()),
                                                               containsString("2")));
  }

  private void assertValid() {
    validator.validate(extensionModel, reporter);
    assertThat(reporter.hasErrors(), is(false));
  }

  private void mockSourceType(Class<? extends Source> sourceType) {
    when(sourceModel.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(
                                                                                       of(new ImplementingTypeModelProperty(sourceType)));
  }

  private static abstract class TestSource extends Source<Void, Void> {

    @Override
    public void onStart(SourceCallback<Void, Void> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  private static class NoFlowInfoSource extends TestSource {

  }

  private static class OneFlowInfoSource extends TestSource {

    private FlowInfo flowInfo;

  }

  private static class TwoFlowInfoSource extends OneFlowInfoSource {

    private FlowInfo secondFlowInfo;
  }
}
