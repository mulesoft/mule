/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import org.codehaus.groovy.control.CompilationFailedException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyShell;

/**
 * Junit rule to run Groovy scripts for testing. Usage:
 * <p>
 *
 * <pre>
 * public class MuleApplicationTestCase {
 *
 *   &#064;ClassRule
 *   public static GroovyScriptExecutor executor = builder(&quot;/path/to/script.groovy&quot;).withProperty("property", "value");
 *
 *   &#064;Test
 *   public void useApplication() throws IOException {
 *     // Groovy script is executed
 *     // This code exercises the application
 *   }
 * }
 * </pre>
 */
public class GroovyScriptExecutor implements TestRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroovyScriptExecutor.class);

  private final String scriptPath;
  private Map<String, String> properties = new HashMap<>();
  private Map<String, Supplier<String>> propertiesUsingLambdas = new HashMap<>();

  public static class Builder {

    private String scriptPath;
    private Map<String, String> properties = new HashMap<>();
    private Map<String, Supplier<String>> propertiesUsingLambdas = new HashMap<>();

    Builder(String scriptPath) {
      this.scriptPath = scriptPath;
    }

    /**
     * Specifies a property to be passed to the Groovy script.
     *
     * @param property
     * @param value
     * @return
     */
    public Builder withProperty(String property, String value) {
      properties.put(property, value);
      return this;
    }

    /**
     * Specifies a property to be passed to the Groovy script that needs to be resolved before its execution.
     *
     * @param property
     * @param propertySupplier
     * @return
     */
    public Builder withPropertyUsingLambda(String property, Supplier<String> propertySupplier) {
      propertiesUsingLambdas.put(property, propertySupplier);
      return this;
    }

    /**
     * Specifies a Map of properties to be passed to the Groovy script.
     *
     * @param properties
     * @return
     */
    public Builder withProperties(Map<String, String> properties) {
      properties.putAll(properties);
      return this;
    }

    /**
     * Creates a new {@link GroovyScriptExecutor} with all the previously defined properties.
     *
     * @return a {@link GroovyScriptExecutor} instance
     */
    public GroovyScriptExecutor build() {
      return new GroovyScriptExecutor(scriptPath, properties, propertiesUsingLambdas);
    }

  }

  /**
   * Creates a new {@link Builder} for the construction of a {@link GroovyScriptExecutor} instance.
   *
   * @param scriptPath the full path of the groovy script to be executed.
   * @return a {@link Builder} instance.
   */
  public static GroovyScriptExecutor.Builder builder(String scriptPath) {
    return new GroovyScriptExecutor.Builder(scriptPath);
  }

  public GroovyScriptExecutor(String scriptPath, Map<String, String> properties,
                              Map<String, Supplier<String>> propertiesUsingLambdas) {
    this(scriptPath);
    this.properties = properties;
    this.propertiesUsingLambdas = propertiesUsingLambdas;
  }

  protected GroovyScriptExecutor(String scriptPath) {
    this.scriptPath = scriptPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Statement apply(Statement base, Description description) {
    return statement(base);
  }

  private Statement statement(final Statement base) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        executeGroovyScript();
        try {
          base.evaluate();
        } finally {
        }
      }
    };
  }

  private void executeGroovyScript() {
    try {
      GroovyShell shell = new GroovyShell();

      resolvePropertiesUsingLambdas();
      properties.forEach((key, value) -> shell.setProperty(key, value));

      shell.evaluate(IOUtils.getResourceAsString(scriptPath, GroovyScriptExecutor.class));

      LOGGER.info("Groovy script executed");

    } catch (IOException e) {
      throw new RuntimeException("Error reading Groovy script: " + scriptPath, e);
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Compilation error were found on: " + scriptPath, e);
    }
  }

  private void resolvePropertiesUsingLambdas() {
    propertiesUsingLambdas
        .forEach((propertyName, propertySupplierLambda) -> properties.put(propertyName, propertySupplierLambda.get()));
  }
}
