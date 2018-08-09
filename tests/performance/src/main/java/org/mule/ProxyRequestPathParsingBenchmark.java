/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.Arrays.copyOfRange;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public class ProxyRequestPathParsingBenchmark extends AbstractBenchmark {

  public static final String LISTENER_PATH_KEY = "listenerPath";
  public static final String REQUEST_PATH_KEY = "requestPath";

  public static final List<PathGroup> pathGroups = ImmutableList.<PathGroup>builder()
      .add(new PathGroup("/*", "/tigres", "/tigres"))
      .add(new PathGroup("/*", "/tigres/", "/tigres/"))
      .add(new PathGroup("/*", "/tres/tristes/tigres", "/tres/tristes/tigres"))
      .add(new PathGroup("/tres/tristes/*", "/tres/tristes/tigres", "/tigres"))
      .add(new PathGroup("/tres/{tristes}/tigres/{comian}/*", "/tres/felices/tigres/tomaban/trigo/en/un/trigal",
                         "/trigo/en/un/trigal"))
      .build();


  private MuleContext muleContext;

  private final String oldDwExpression =
      "vars.requestPath replace ((((vars.listenerPath replace '/\\*' as Regex with '') replace /\\{\\w+\\}/ with '\\\\w+') as Regex replace '/' with '\\\\/')) as Regex with ''";

  private final String newDwExpression = "do {\n" +
      "    var postfix: String = if(vars.requestPath endsWith '/') '/' else ''\n" +
      "    var requestPathParts = vars.requestPath splitBy '/'\n" +
      "    var listenerPathParts = vars.listenerPath splitBy '/'\n" +
      "    ---\n" +
      "    '/' ++ joinBy(requestPathParts[sizeOf(listenerPathParts) - 1 to -1], '/') ++ postfix\n" +
      "}";

  private CoreEvent simplePathEvent;
  private CoreEvent pathEndingWithSlashEvent;
  private CoreEvent wildcardResolvesToMultipleParamsEvent;
  private CoreEvent paramsBeforeWildcardEvent;
  private CoreEvent uriParamsEvent;


  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();
    CoreEvent dummyEvent = createEvent(createFlow(muleContext));
    simplePathEvent = CoreEvent.builder(dummyEvent).addVariable(LISTENER_PATH_KEY, pathGroups.get(0).getListenerPath())
        .addVariable(REQUEST_PATH_KEY, pathGroups.get(0).getRequestPath()).build();
    pathEndingWithSlashEvent = CoreEvent.builder(dummyEvent).addVariable(LISTENER_PATH_KEY, pathGroups.get(1).getListenerPath())
        .addVariable(REQUEST_PATH_KEY, pathGroups.get(1).getRequestPath()).build();
    wildcardResolvesToMultipleParamsEvent =
        CoreEvent.builder(dummyEvent).addVariable(LISTENER_PATH_KEY, pathGroups.get(2).getListenerPath())
            .addVariable(REQUEST_PATH_KEY, pathGroups.get(2).getRequestPath()).build();
    paramsBeforeWildcardEvent = CoreEvent.builder(dummyEvent).addVariable(LISTENER_PATH_KEY, pathGroups.get(3).getListenerPath())
        .addVariable(REQUEST_PATH_KEY, pathGroups.get(3).getRequestPath()).build();
    uriParamsEvent = CoreEvent.builder(dummyEvent).addVariable(LISTENER_PATH_KEY, pathGroups.get(4).getListenerPath())
        .addVariable(REQUEST_PATH_KEY, pathGroups.get(4).getRequestPath()).build();
  }

  @TearDown
  public void teardown() throws MuleException {
    muleContext.stop();
    stopIfNeeded(lookupObject(muleContext, SchedulerService.class));
    muleContext.dispose();
  }

  @Method
  public String withOldDwExpression(CoreEvent event) {
    return (String) muleContext.getExpressionManager().evaluate(oldDwExpression, event).getValue();
  }

  @Benchmark
  public String oldDwExpressionWithEvent0() {
    return withOldDwExpression(simplePathEvent);
  }

  @Benchmark
  public String oldDwExpressionWithEvent1() {
    return withOldDwExpression(pathEndingWithSlashEvent);
  }

  @Benchmark
  public String oldDwExpressionWithEvent2() {
    return withOldDwExpression(wildcardResolvesToMultipleParamsEvent);
  }

  @Benchmark
  public String oldDwExpressionWithEvent3() {
    return withOldDwExpression(paramsBeforeWildcardEvent);
  }

  @Benchmark
  public String oldDwExpressionWithEvent4() {
    return withOldDwExpression(uriParamsEvent);
  }

  @Method
  public String withNewDwExpression(CoreEvent event) {
    return (String) muleContext.getExpressionManager().evaluate(newDwExpression, event).getValue();
  }

  @Benchmark
  public String newDwExpressionWithEvent0() {
    return withNewDwExpression(simplePathEvent);
  }

  @Benchmark
  public String newDwExpressionWithEvent1() {
    return withNewDwExpression(pathEndingWithSlashEvent);
  }

  @Benchmark
  public String newDwExpressionWithEvent2() {
    return withNewDwExpression(wildcardResolvesToMultipleParamsEvent);
  }

  @Benchmark
  public String newDwExpressionWithEvent3() {
    return withNewDwExpression(paramsBeforeWildcardEvent);
  }

  @Benchmark
  public String newDwExpressionWithEvent4() {
    return withNewDwExpression(uriParamsEvent);
  }

  @Method
  public String withSplitBy(CoreEvent event) {
    String listenerPath = (String) event.getVariables().get(LISTENER_PATH_KEY).getValue();
    String requestPath = (String) event.getVariables().get(REQUEST_PATH_KEY).getValue();
    String postChar = requestPath.getBytes()[requestPath.length() - 1] == '/' ? "/" : "";
    String[] splittedListener = listenerPath.split("/");
    String[] splittedRequest = requestPath.split("/");
    String[] slicedSplittedRequest = copyOfRange(splittedRequest, splittedListener.length - 1, splittedRequest.length);
    return "/" + String.join("/", slicedSplittedRequest) + postChar;
  }

  @Benchmark
  public String splitByWithEvent0() {
    return withSplitBy(simplePathEvent);
  }

  @Benchmark
  public String splitByWithEvent1() {
    return withSplitBy(pathEndingWithSlashEvent);
  }

  @Benchmark
  public String splitByWithEvent2() {
    return withSplitBy(wildcardResolvesToMultipleParamsEvent);
  }

  @Benchmark
  public String splitByWithEvent3() {
    return withSplitBy(paramsBeforeWildcardEvent);
  }

  @Benchmark
  public String splitByWithEvent4() {
    return withSplitBy(uriParamsEvent);
  }

  @Method
  public String iterating(CoreEvent event) {
    String listenerPath = (String) event.getVariables().get(LISTENER_PATH_KEY).getValue();
    String requestPath = (String) event.getVariables().get(REQUEST_PATH_KEY).getValue();
    byte[] listenerPathBytes = listenerPath.getBytes();
    byte[] requestPathBytes = requestPath.getBytes();
    int listenerPathIndex = 0;
    int requestPathIndex = 0;
    while (listenerPathIndex < listenerPathBytes.length - 1) {
      listenerPathIndex = iterateUriParameter(listenerPathBytes, listenerPathIndex);
      requestPathIndex = iterateUriParameter(requestPathBytes, requestPathIndex);
    }
    return requestPath.substring(requestPathIndex - 1);
  }

  private int iterateUriParameter(byte[] bytes, int position) {
    while (bytes[position] != '/') {
      position++;
    }
    position++;
    return position;
  }

  @Benchmark
  public String iteratingWithEvent0() {
    return iterating(simplePathEvent);
  }

  @Benchmark
  public String iteratingWithEvent1() {
    return iterating(pathEndingWithSlashEvent);
  }

  @Benchmark
  public String iteratingWithEvent2() {
    return iterating(wildcardResolvesToMultipleParamsEvent);
  }

  @Benchmark
  public String iteratingWithEvent3() {
    return iterating(paramsBeforeWildcardEvent);
  }

  @Benchmark
  public String iteratingWithEvent4() {
    return iterating(uriParamsEvent);
  }

  public static class PathGroup {

    private String listenerPath;
    private String requestPath;
    private String expectedProxyRequestPath;

    public PathGroup(String listenerPath, String requestPath, String expectedProxyRequestPath) {
      this.listenerPath = listenerPath;
      this.requestPath = requestPath;
      this.expectedProxyRequestPath = expectedProxyRequestPath;
    }

    public String getListenerPath() {
      return listenerPath;
    }

    public String getRequestPath() {
      return requestPath;
    }

    public String getExpectedProxyRequestPath() {
      return expectedProxyRequestPath;
    }
  }

  @Target({ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Method {

  }
}
