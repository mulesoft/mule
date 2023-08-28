/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
