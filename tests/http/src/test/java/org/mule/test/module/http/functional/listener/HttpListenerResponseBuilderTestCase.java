/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.CREATED;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;

import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerResponseBuilderTestCase extends AbstractHttpTestCase {

  private static final String FAIL = "fail";
  public static final int TIMEOUT = 100000;

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");
  @Rule
  public SystemProperty emptyResponseBuilderPath = new SystemProperty("emptyResponseBuilderPath", "emptyResponseBuilderPath");
  @Rule
  public SystemProperty statusResponseBuilderPath = new SystemProperty("statusResponseBuilderPath", "statusResponseBuilderPath");
  @Rule
  public SystemProperty headerResponseBuilderPath = new SystemProperty("headerResponseBuilderPath", "headerResponseBuilderPath");
  @Rule
  public SystemProperty headersResponseBuilderPath =
      new SystemProperty("headersResponseBuilderPath", "headersResponseBuilderPath");
  @Rule
  public SystemProperty headersOverrideResponseBuilderPath =
      new SystemProperty("headersOverrideResponseBuilderPath", "headersOverrideResponseBuilderPath");
  @Rule
  public SystemProperty defaultReasonPhraseResponseBuilderPath =
      new SystemProperty("defaultReasonPhraseResponseBuilderPath", "defaultReasonPhraseResponseBuilderPath");
  @Rule
  public SystemProperty noReasonPhraseUnknownStatusCodeResponseBuilderPath =
      new SystemProperty("noReasonPhraseUnknownStatusCodeResponseBuilderPath",
                         "noReasonPhraseUnknownStatusCodeResponseBuilderPath");
  @Rule
  public SystemProperty errorEmptyResponseBuilderPath =
      new SystemProperty("errorEmptyResponseBuilderPath", "errorEmptyResponseBuilderPath");
  @Rule
  public SystemProperty errorStatusResponseBuilderPath =
      new SystemProperty("errorStatusResponseBuilderPath", "errorStatusResponseBuilderPath");
  @Rule
  public SystemProperty errorHeaderResponseBuilderPath =
      new SystemProperty("errorHeaderResponseBuilderPath", "errorHeaderResponseBuilderPath");
  @Rule
  public SystemProperty errorHeadersResponseBuilderPath =
      new SystemProperty("errorHeadersResponseBuilderPath", "errorHeadersResponseBuilderPath");
  @Rule
  public SystemProperty errorHeadersOverrideResponseBuilderPath =
      new SystemProperty("errorHeadersOverrideResponseBuilderPath", "errorHeadersOverrideResponseBuilderPath");
  @Rule
  public SystemProperty responseBuilderAndErrorResponseBuilderNotTheSamePath =
      new SystemProperty("responseBuilderAndErrorResponseBuilderNotTheSamePath",
                         "responseBuilderAndErrorResponseBuilderNotTheSamePath");

  @Override
  protected String getConfigFile() {
    return "http-listener-response-builder-config.xml";
  }

  @Test
  public void emptyResponseBuilder() throws Exception {
    final String url = getUrl(emptyResponseBuilderPath);
    emptyResponseBuilderTest(url, 200);
  }

  @Test
  public void statusLineResponseBuilder() throws Exception {
    final String url = getUrl(statusResponseBuilderPath);
    HttpResponse httpResponse = statusLineResponseBuilderTest(url, CREATED.getStatusCode());
    assertThat(httpResponse.getFirstHeader(CONTENT_LENGTH).getValue(), is("0"));
  }

  @Test
  public void headerResponseBuilder() throws Exception {
    final String url = getUrl(headerResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void headersResponseBuilder() throws Exception {
    final String url = getUrl(headersResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void headersOverrideResponseBuilder() throws Exception {
    final String url = getUrl(headersOverrideResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void setReasonPhraseWhenStatusCodeIsAvailable() throws Exception {
    final String url = getUrl(defaultReasonPhraseResponseBuilderPath);
    statusLineResponseBuilderTest(url, OK.getStatusCode(), OK.getReasonPhrase());
  }

  @Test
  public void noReasonPhraseWhenStatusCodeIsNotAndExpectedHttpStatus() throws Exception {
    final String url = getUrl(noReasonPhraseUnknownStatusCodeResponseBuilderPath);
    statusLineResponseBuilderTest(url, 1001, "");
  }

  @Test
  public void errorEmptyResponseBuilder() throws Exception {
    final String url = getUrl(errorEmptyResponseBuilderPath);
    emptyResponseBuilderTest(url, 500);
  }

  @Test
  public void errorStatusLineResponseBuilder() throws Exception {
    final String url = getUrl(errorStatusResponseBuilderPath);
    HttpResponse httpResponse = statusLineResponseBuilderTest(url, CREATED.getStatusCode());
    // we'll get the Exception message in the body
    assertThat(httpResponse.getFirstHeader(CONTENT_LENGTH).getValue(), is(not("0")));
  }

  @Test
  public void errorHeaderResponseBuilder() throws Exception {
    final String url = getUrl(errorHeaderResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void errorHeadersResponseBuilder() throws Exception {
    final String url = getUrl(errorHeadersResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void errorHeadersOverrideResponseBuilder() throws Exception {
    final String url = getUrl(errorHeadersOverrideResponseBuilderPath);
    simpleHeaderTest(url);
  }

  @Test
  public void responseBuilderIsDifferentFromErrorResponseBuilder() throws Exception {
    final String url = getUrl(responseBuilderAndErrorResponseBuilderNotTheSamePath);
    final Response successfulResponse = Request.Get(url).connectTimeout(TIMEOUT).socketTimeout(10000000).execute();
    assertThat(successfulResponse.returnResponse().getStatusLine().getStatusCode(), is(202));
    final Response failureResponse =
        Request.Get(url).addHeader(FAIL, "true").connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute();
    assertThat(failureResponse.returnResponse().getStatusLine().getStatusCode(), is(505));
  }

  private String getUrl(SystemProperty pathSystemProperty) {
    return String.format("http://localhost:%s/%s", listenPort.getNumber(), pathSystemProperty.getValue());
  }

  private HttpResponse statusLineResponseBuilderTest(String url, int expectedStatus, String expectedReasonPhrase)
      throws IOException {
    final Response response = Request.Get(url).connectTimeout(DEFAULT_TIMEOUT).execute();
    final HttpResponse httpResponse = response.returnResponse();
    System.out.println(ArrayUtils.toString(httpResponse.getAllHeaders()));
    assertThat(httpResponse.getAllHeaders().length, is(2));
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(expectedStatus));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(expectedReasonPhrase));
    return httpResponse;
  }

  private HttpResponse statusLineResponseBuilderTest(String url, int expectedStatus) throws IOException {
    return statusLineResponseBuilderTest(url, expectedStatus, "everything works!");
  }

  private void emptyResponseBuilderTest(String url, int expectedStatusCode) throws IOException {
    final Response response = Request.Get(url).connectTimeout(DEFAULT_TIMEOUT).execute();
    assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(expectedStatusCode));
  }

  private void simpleHeaderTest(String url) throws IOException {
    final Response response = Request.Get(url).connectTimeout(DEFAULT_TIMEOUT).execute();
    final HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getFirstHeader(HttpHeaders.Names.USER_AGENT).getValue(), is("Mule 3.6.0"));
    assertThat(httpResponse.getFirstHeader(HttpHeaders.Names.DATE).getValue(), new TypeSafeMatcher<String>() {

      private ParseException parseException;

      @Override
      public void describeTo(Description description) {
        description.appendText(parseException.getMessage());
      }

      @Override
      protected boolean matchesSafely(String dateToValidate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
          sdf.parse(dateToValidate);
        } catch (ParseException e) {
          this.parseException = e;
          e.printStackTrace();
          return false;
        }
        return true;
      }
    });
  }
}
