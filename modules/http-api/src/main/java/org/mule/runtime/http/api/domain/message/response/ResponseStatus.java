/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.message.response;

class ResponseStatus {

  private int statusCode;
  private String reasonPhrase;

  ResponseStatus() {
    this.statusCode = 200;
    this.reasonPhrase = "";
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

}
