/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;


import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.tls.TlsContextFactory;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class PetStoreClient {

  private String username;
  private String password;
  private TlsContextFactory tlsContext;
  private String configName;
  private int disconnectCount;
  private Date openingDate;
  private List<Date> closedForHolidays;

  private List<LocalDateTime> discountDates;

  public PetStoreClient(String username, String password, TlsContextFactory tlsContextFactory, String configName,
                        Date openingDate, List<Date> closedForHolidays, List<LocalDateTime> discountDates) {
    this.username = username;
    this.password = password;
    this.tlsContext = tlsContextFactory;
    this.configName = configName;
    this.openingDate = openingDate;
    this.closedForHolidays = closedForHolidays;
    this.discountDates = discountDates;
  }

  public List<String> getPets(String ownerName, PetStoreConnector config) {
    checkArgument(ownerName.equals(username), "config doesn't match");
    return config.getPets();
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void disconnect() {
    disconnectCount++;
  }

  public int getDisconnectCount() {
    return disconnectCount;
  }

  public boolean hasActiveConnection() {
    checkState(disconnectCount >= 0, "negative disconnectCount");
    return disconnectCount == 0;
  }

  public TlsContextFactory getTlsContext() {
    return tlsContext;
  }

  public String getConfigName() {
    return configName;
  }

  public Date getOpeningDate() {
    return openingDate;
  }

  public List<Date> getClosedForHolidays() {
    return closedForHolidays;
  }

  public List<LocalDateTime> getDiscountDates() {
    return discountDates;
  }
}
