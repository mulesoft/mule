/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.api;

/**
 * Enables {@link ToolingService} injection.
 *
 * @since 4.0
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link ToolingService}
 */
@Deprecated
public interface ToolingServiceAware {

  /**
   * @param toolingService the tooling service provided by the container
   */
  void setToolingService(ToolingService toolingService);

}
