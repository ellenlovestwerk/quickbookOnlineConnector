package com.helphalf.quickbook.qbo;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;

/**
 *
 * @author dderose
 *
 */

public class DataServiceFactory {


	/**
	 * Initializes DataService for a given app/company profile
	 *
	 * @return
	 * @throws FMSException
	 */
//	public static DataService getDataService() throws FMSException {
//		//create dataservice
//
//		return new DataService(ContextFactory.getContext());
//	}

	public DataService getDataService(String realmId, String accessToken) throws FMSException {

		//create oauth object
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		//create context
		Context context = new Context(oauth, ServiceType.QBO, realmId);

		// create dataservice
		return new DataService(context);
	}
}
