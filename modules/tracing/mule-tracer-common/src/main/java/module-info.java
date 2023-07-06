/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Definitions for Mule tracer.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.tracer.common {
    exports org.mule.runtime.tracer.common.watcher;

    requires org.mule.runtime.tracer.exporter.configuration.api;
    requires org.slf4j;
}
