package com.helphalf.quickbook.helper;

import com.intuit.ipp.data.EmailAddress;

/**
 * @author dderose
 *
 */
public final class Email {
	
	private Email() {
		
	}

	public static EmailAddress getEmailAddress() {
		EmailAddress emailAddr = new EmailAddress();
		emailAddr.setAddress("test@abc.com");
		return emailAddr;
	}

}
