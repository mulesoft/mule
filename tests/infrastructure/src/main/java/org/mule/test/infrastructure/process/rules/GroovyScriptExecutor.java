/*
 * (c) 2003-2017 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.infrastructure.process.rules;

import org.mule.runtime.core.api.util.IOUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyShell;

/**
 * Junit rule to run Groovy scripts.
 */
public class GroovyScriptExecutor extends ExternalResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleDeployment.class);

  private final String scriptPath;
  private Map<String, String> properties = new HashMap<>();
  private Map<String, Supplier<String>> propertiesUsingLambdas = new HashMap<>();

  public static class Builder {

    GroovyScriptExecutor executor;

    Builder(String scriptPath) {
      executor = new GroovyScriptExecutor(scriptPath);
    }

    public Builder withProperty(String property, String value) {
      executor.properties.put(property, value);
      return this;
    }

    public Builder withPropertyUsingLambda(String property, Supplier<String> propertySupplier) {
      executor.propertiesUsingLambdas.put(property, propertySupplier);
      return this;
    }

    public Builder withProperties(Map<String, String> properties) {
      executor.properties.putAll(properties);
      return this;
    }

    public GroovyScriptExecutor build() {
      return executor;
    }
  }

  public static GroovyScriptExecutor.Builder builder(String scriptPath) {
    return new GroovyScriptExecutor.Builder(scriptPath);
  }

  protected GroovyScriptExecutor(String scriptPath) {
    this.scriptPath = scriptPath;
  }

  @Override
  protected void before() throws Throwable {
    executeGroovyScript();
  }

  private void executeGroovyScript() {
    try {
      GroovyShell shell = new GroovyShell();

      resolvePropertiesUsingLambdas();
      properties.forEach((key, value) -> shell.setProperty(key, value));

      shell.evaluate(IOUtils.getResourceAsString(scriptPath, GroovyScriptExecutor.class));

      LOGGER.info("Groovy script executed");

    } catch (Exception e) {
      String errorMessage = "Error executing Groovy script: " + scriptPath;
      LOGGER.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private void resolvePropertiesUsingLambdas() {
    propertiesUsingLambdas
        .forEach((propertyName, propertySupplierLambda) -> properties.put(propertyName, propertySupplierLambda.get()));
  }
}
