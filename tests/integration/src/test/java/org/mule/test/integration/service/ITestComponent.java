package org.mule.test.integration.service;

/**
 * Interface for TestComponent (to make it easy to host the service on Axis
 * 
 * @author Alan Cassar
 *
 */
public interface ITestComponent {
	
	public String receive(String message) throws Exception;

}
