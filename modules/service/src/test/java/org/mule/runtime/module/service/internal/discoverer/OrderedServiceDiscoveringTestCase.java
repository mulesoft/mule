package org.mule.runtime.module.service.internal.discoverer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class OrderedServiceDiscoveringTestCase {

  private static final String SCHEDULER_SERVICE_ARTIFACT_NAME = "Scheduler service";

  private MockService mockSchedulerService = new MockService(SCHEDULER_SERVICE_ARTIFACT_NAME);

  private List<Service> mockedServices;

  private List<Service> buildMockServicesList() {
    return Arrays.asList(
                         new MockService("AwesomeService1"),
                         mockSchedulerService,
                         new MockService("AwesomeService2"));
  }

  @Before
  public void setUp() throws Exception {
    mockedServices = buildMockServicesList();
  }

 @Test
  public void allDiscoveredServicesAreContainedAfterReordering() throws ServiceResolutionError {
    List<Service> orderedServices = discoverAndSortServices();
    assertThat(orderedServices.containsAll(buildMockServicesList()), is(true));
  }

  @Test
  public void schedulerServiceIsPushedBackOnDiscovering() throws ServiceResolutionError {
    List<Service> orderedServices = discoverAndSortServices();
    assertThat(orderedServices.get(orderedServices.size() - 1), is(mockSchedulerService));
  }

  protected List<Service> discoverAndSortServices() throws ServiceResolutionError {
    ServiceDiscoverer mockServiceDiscoverer = mock(ServiceDiscoverer.class);
    when(mockServiceDiscoverer.discoverServices()).thenReturn(mockedServices);
    ServiceDiscoverer orderingServiceDiscoverer = new OrderingServiceDiscovererWrapper(mockServiceDiscoverer);

    return (List<Service>) orderingServiceDiscoverer.discoverServices();
  }

  private class MockService implements Service {

    private String name;

    public MockService(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MockService &&
          ((MockService) obj).getName().equals(name);
    }
  }
}
