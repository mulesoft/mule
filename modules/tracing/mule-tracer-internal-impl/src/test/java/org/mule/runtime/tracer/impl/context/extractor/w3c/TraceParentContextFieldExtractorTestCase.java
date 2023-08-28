/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.context.extractor.w3c;

import static org.mule.runtime.tracer.impl.context.extractor.w3c.TraceParentContextFieldExtractor.TRACEPARENT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mule.runtime.tracer.impl.context.extractor.TraceContextFieldExtractor;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class TraceParentContextFieldExtractorTestCase extends AbstractW3CTraceContextExtractorTestCase {

  @Override
  public String getTraceField() {
    return TRACEPARENT;
  }

  @Override
  public TraceContextFieldExtractor getTraceContextFieldExtractor() {
    return new TraceParentContextFieldExtractor();
  }
}
