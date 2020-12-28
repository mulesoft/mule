/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.test.heisenberg.extension.exception.ValidationErrorTypeProvider;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.stereotypes.EmpireStereotype;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Stereotype(EmpireStereotype.class)
public class MoneyLaunderingOperation {

  public static final List<PersonalInfo> INVOLVED_PEOPLE = asList(
                                                                  new PersonalInfo("Skyler", 34),
                                                                  new PersonalInfo("BabySkyler", 0),
                                                                  new PersonalInfo("Walter Jr", 17),
                                                                  new PersonalInfo("Walter", 50),
                                                                  new PersonalInfo("Lydia", 33),
                                                                  new PersonalInfo("Mike", 62),
                                                                  new PersonalInfo("Jesse", 21),
                                                                  new PersonalInfo("Saul", 49),
                                                                  new PersonalInfo("Marie", 34),
                                                                  new PersonalInfo("Gus", 45),
                                                                  new PersonalInfo("Tood", 22));

  public static Integer closeEmptyOperationCalls = 0;
  public static Integer closePagingProviderCalls = 0;
  public static Integer getPageCalls = 0;

  private long totalLaunderedAmount = 0;

  public synchronized Long launder(@Config HeisenbergExtension config, long amount) {
    config.setMoney(config.getMoney().subtract(BigDecimal.valueOf(amount)));
    totalLaunderedAmount += amount;
    return totalLaunderedAmount;
  }

  public PagingProvider<HeisenbergConnection, PersonalInfo> getPagedPersonalInfo() {
    return new PagingProvider<HeisenbergConnection, PersonalInfo>() {

      private int index = 0;
      private int timesClosed = 0;

      @Override
      public List<PersonalInfo> getPage(HeisenbergConnection heisenbergConnection) {
        List<PersonalInfo> page = new ArrayList<>();
        for (int i = 0; i < 2 && index < INVOLVED_PEOPLE.size(); i++) {
          page.add(INVOLVED_PEOPLE.get(index++));
        }
        return page;
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection heisenbergConnection) {
        return Optional.of(INVOLVED_PEOPLE.size());
      }

      @Override
      public void close(HeisenbergConnection connection) throws MuleException {
        timesClosed++;
        if (timesClosed > 1) {
          throw new RuntimeException("Expected to be closed only once but was called twice");
        }
      }
    };
  }

  public PagingProvider<HeisenbergConnection, String> emptyPagedOperation() {
    return new PagingProvider<HeisenbergConnection, String>() {

      private int timesClosed = 0;

      @Override
      public List<String> getPage(HeisenbergConnection connection) {
        return emptyList();
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return Optional.of(0);
      }

      @Override
      public void close(HeisenbergConnection connection) throws MuleException {
        timesClosed++;
        closeEmptyOperationCalls = timesClosed;
        if (timesClosed > 1) {
          throw new RuntimeException("Expected to be closed only once but was called twice");
        }
      }
    };
  }

  public PagingProvider<HeisenbergConnection, String> failingPagedOperation() {
    return new PagingProvider<HeisenbergConnection, String>() {

      @Override
      public List<String> getPage(HeisenbergConnection connection) {
        throw new IllegalArgumentException();
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return Optional.of(0);
      }

      @Override
      public void close(HeisenbergConnection connection) throws MuleException {}
    };
  }

  public PagingProvider<HeisenbergConnection, String> pagedOperationUsingConnection() {
    return new PagingProvider<HeisenbergConnection, String>() {

      private int timesClosed = 0;
      int index = 0;

      @Override
      public List<String> getPage(HeisenbergConnection connection) {

        if (index > 3) {
          return emptyList();
        }

        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
          numbers.add(connection.getSaulPhoneNumber());
        }
        index++;
        return numbers;
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return Optional.of(4);
      }

      @Override
      public void close(HeisenbergConnection connection) throws MuleException {
        index = 0;
        timesClosed++;
        if (timesClosed > 1) {
          throw new RuntimeException("Expected to be closed only once but was called twice");
        }
      }
    };
  }

  public PagingProvider<HeisenbergConnection, Integer> stickyPagedOperation() {
    return new PagingProvider<HeisenbergConnection, Integer>() {

      private int timesClosed = 0;

      @Override
      public List<Integer> getPage(HeisenbergConnection connection) {
        return asList(System.identityHashCode(connection));
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return empty();
      }

      @Override
      public void close(HeisenbergConnection connection) throws MuleException {
        timesClosed++;
        if (timesClosed > 1) {
          throw new RuntimeException("Expected to be closed only once but was called twice");
        }
      }

      @Override
      public boolean useStickyConnections() {
        return true;
      }
    };
  }

  public PagingProvider<HeisenbergConnection, Integer> pagedOperationWithExtensionClassLoader() {
    return new PagingProvider<HeisenbergConnection, Integer>() {

      @Override
      public List<Integer> getPage(HeisenbergConnection heisenbergConnection) {
        if (Thread.currentThread().getContextClassLoader() != getClass().getClassLoader()) {
          throw new RuntimeException("Class Loader was not properly switched by the Paging Provider Wrapper");
        }
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        return list;
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection heisenbergConnection) {
        return empty();
      }

      @Override
      public void close(HeisenbergConnection heisenbergConnection) throws MuleException {
        heisenbergConnection.disconnect();
      }
    };
  }

  public PagingProvider<HeisenbergConnection, Integer> failAtClosePagedOperation(Integer failOn) {
    return new PagingProvider<HeisenbergConnection, Integer>() {

      @Override
      public List<Integer> getPage(HeisenbergConnection connection) {
        getPageCalls++;
        if (getPageCalls == failOn) {
          throw new ModuleException(CONNECTIVITY, new ConnectionException("Failed to retrieve Page"));
        }
        if (getPageCalls > 2) {
          return emptyList();
        }
        return singletonList(0);
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return empty();
      }

      @Override
      public void close(HeisenbergConnection connection) {
        closePagingProviderCalls++;
        throw new IllegalArgumentException("Failed to close Paging Provider.");
      }
    };
  }

  public PagingProvider<HeisenbergConnection, Integer> failingConnectivityPagedOperation(Integer failOn) {
    return new PagingProvider<HeisenbergConnection, Integer>() {

      @Override
      public List<Integer> getPage(HeisenbergConnection connection) {
        getPageCalls++;
        if (getPageCalls.equals(failOn)) {
          throw new ModuleException(CONNECTIVITY, new ConnectionException("Failed to retrieve Page"));
        }
        if (getPageCalls > 3) {
          return emptyList();
        }
        return singletonList(0);
      }

      @Override
      public Optional<Integer> getTotalResults(HeisenbergConnection connection) {
        return empty();
      }

      @Override
      public void close(HeisenbergConnection connection) {
        closePagingProviderCalls++;
      }
    };
  }

  @Validator
  @Throws(ValidationErrorTypeProvider.class)
  public void validateMoney() {

  }
}
