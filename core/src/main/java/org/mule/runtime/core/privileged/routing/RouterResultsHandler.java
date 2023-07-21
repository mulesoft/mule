/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.List;

/**
 * An SPI interface where custom logic can be plugged in to control how collections and single messages are returned from routers.
 */
@NoImplement
public interface RouterResultsHandler {

  CoreEvent aggregateResults(List<CoreEvent> results, CoreEvent previous);
}
