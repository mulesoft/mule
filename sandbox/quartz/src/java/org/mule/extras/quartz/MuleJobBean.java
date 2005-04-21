package org.mule.extras.quartz;

import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class MuleJobBean extends QuartzJobBean {

	private String endpoint;
	private String muleManager;
	private Object payload;
	private boolean synchronous = false;
	
	public MuleJobBean() {
	}

	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			MuleClient mc = new MuleClient();
			if (muleManager != null) {
				RemoteDispatcher rd = mc.getRemoteDispatcher(muleManager);
				if (synchronous) {
					rd.sendRemote(endpoint, payload, null);
				} else {
					rd.dispatchRemote(endpoint, payload, null);
				}
			} else {
				if (synchronous) {
					mc.send(endpoint, payload, null);
				} else {
					mc.dispatch(endpoint, payload, null);
				}
			}
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	/**
	 * @return Returns the endpoint.
	 */
	public String getEndpoint() {
		return endpoint;
	}
	

	/**
	 * @param endpoint The endpoint to set.
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	

	/**
	 * @return Returns the muleManager.
	 */
	public String getMuleManager() {
		return muleManager;
	}
	

	/**
	 * @param muleManager The muleManager to set.
	 */
	public void setMuleManager(String muleManager) {
		this.muleManager = muleManager;
	}
	

	/**
	 * @return Returns the payload.
	 */
	public Object getPayload() {
		return payload;
	}
	

	/**
	 * @param payload The payload to set.
	 */
	public void setPayload(Object payload) {
		this.payload = payload;
	}

	/**
	 * @param synchronous The synchronous to set.
	 */
	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	/**
	 * @return Returns the synchronous.
	 */
	public boolean isSynchronous() {
		return synchronous;
	}
	
	
	

}
