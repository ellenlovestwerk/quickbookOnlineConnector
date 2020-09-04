package com.helphalf.quickbook.helper;

import com.intuit.ipp.data.EmailAddress;

/**
 * @author dderose
 *
 */
public final class Email {
	
	private Email() {
		
	}

	public static EmailAddress getEmailAddressWithAddr(String email) {
		EmailAddress emailAddr = new EmailAddress();
		emailAddr.setAddress(email);
		return emailAddr;
	}
	public static EmailAddress getEmailAddress() {
		EmailAddress emailAddr = new EmailAddress();
		emailAddr.setAddress("test@gmail.com");
		return emailAddr;
	}

}
