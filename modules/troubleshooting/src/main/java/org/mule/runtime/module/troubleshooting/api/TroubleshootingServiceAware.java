/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

/**
 * Enables {@link TroubleshootingService} injection.
 *
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link TroubleshootingService}
 */
@Deprecated
@Experimental
public interface TroubleshootingServiceAware {

  void setTroubleshootingService(TroubleshootingService troubleshootingService);
}
