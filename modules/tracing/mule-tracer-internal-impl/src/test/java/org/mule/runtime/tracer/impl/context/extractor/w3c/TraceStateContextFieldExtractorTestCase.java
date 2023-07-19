/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor.w3c;

import static org.mule.runtime.tracer.impl.context.extractor.w3c.TraceStateContextFieldExtractor.TRACESTATE;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import org.mule.runtime.tracer.impl.context.extractor.TraceContextFieldExtractor;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class TraceStateContextFieldExtractorTestCase extends AbstractW3CTraceContextExtractorTestCase {

  @Override
  public String getTraceField() {
    return TRACESTATE;
  }

  @Override
  public TraceContextFieldExtractor getTraceContextFieldExtractor() {
    return new TraceStateContextFieldExtractor();
  }
}
