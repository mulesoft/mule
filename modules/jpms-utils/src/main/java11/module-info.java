/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * This module provides JMPS utilities for use within the Mule Runtime.
 * 
 * @moduleGraph
 * @since 1.5
 */
module org.mule.runtime.jpms.utils {

  requires java.management;

  exports org.mule.runtime.jpms.api;

}
