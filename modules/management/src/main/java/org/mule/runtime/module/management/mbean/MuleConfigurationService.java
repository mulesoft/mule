/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.mbean;

import static org.mule.runtime.api.exception.ExceptionHelper.fullStackTraces;
import static org.mule.runtime.api.exception.ExceptionHelper.stackTraceFilter;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.api.exception.ExceptionHelper;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration settings as a management service.
 * 
 */
public class MuleConfigurationService implements MuleConfigurationServiceMBean {

  private MuleConfiguration muleConfiguration;

  public MuleConfigurationService(MuleConfiguration muleConfiguration) {
    this.muleConfiguration = muleConfiguration;
  }

  public int getSynchronousEventTimeout() {
    return muleConfiguration.getDefaultResponseTimeout();
  }

  public String getWorkingDirectory() {
    return muleConfiguration.getWorkingDirectory();
  }

  public int getTransactionTimeout() {
    return muleConfiguration.getDefaultTransactionTimeout();
  }

  public int getShutdownTimeout() {
    return muleConfiguration.getShutdownTimeout();
  }

  public boolean isClientMode() {
    return muleConfiguration.isClientMode();
  }


  public String getEncoding() {
    return muleConfiguration.getDefaultEncoding();
  }

  public boolean isContainerMode() {
    return muleConfiguration.isContainerMode();
  }

  public boolean isFullStackTraces() {
    /*
     * Sacrifice the code quality for the sake of keeping things simple - the alternative would be to pass MuleContext into every
     * exception constructor.
     */
    return fullStackTraces;
  }

  public void setFullStackTraces(boolean flag) {
    /*
     * Sacrifice the code quality for the sake of keeping things simple - the alternative would be to pass MuleContext into every
     * exception constructor.
     */
    ExceptionHelper.fullStackTraces = flag;
  }

  public String getStackTraceFilter() {
    return StringUtils.join(stackTraceFilter, ',');
  }

  public void setStackTraceFilter(String filterAsString) {
    stackTraceFilter = filterAsString.split(",");
  }
}
