/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.profiling.tracing;

import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * Interface which allows classes to set {@link org.mule.runtime.tracer.api.span.info.InitialSpanInfo}
 *
 * @since 4.5.0
 */
public interface InitialSpanInfoAware {

  void setInitialSpanInfo(InitialSpanInfo initialSpanInfo);
}
