package com.helphalf.quickbook.qbo;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.util.Logger;

/**
 *
 * @author dderose
 *
 */

public class ContextFactory {

	private static final org.slf4j.Logger LOG = Logger.getLogger();

	public static String bearerToken = "";
	public static String companyID = "";
	public static String refreshToken = "";

//	private  static Properties prop;

	/**
	 * Initializes Context for a given app/company profile
	 *
	 * @return
	 * @throws FMSException
	 */
	public static Context getContext() throws FMSException {

		//create context
		OAuth2Authorizer oauth = new OAuth2Authorizer(bearerToken);
//		System.out.println("bearerToken--------------!!!!!!!!!!"+bearerToken);
//		System.out.println("realm ID--------------!!!!!!!!!!"+companyID);

		Context context = new Context(oauth, ServiceType.QBO, companyID);
//		Context context = new Context(oauth, companyID);



//		System.out.println("context---------!"+context+" ----   qbo    --"+context.getIntuitServiceType()+"realmID-------!!"+context.getRealmID());
		return context;
	}

}
