package com.helphalf.quickbook.helper;


import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.DateUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
 * @author dderose
 *
 */
public final class CustomerHelper {
	
	private CustomerHelper() {
		
	}

	public static Customer getCustomer(DataService service) throws FMSException, ParseException {
		List<Customer> customers = (List<Customer>) service.findAll(new Customer());
//		System.out.println("customers"+customers);
		if (!customers.isEmpty()) {
			return customers.get(0);
		}
		return createCustomer(service);
	}

	public static Customer getCustomerWithId(DataService service, String id) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from Customer Where id = '"+id+"'");
		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			Customer customer = (Customer) queryResult.getEntities().get(0);
			return customer;
		}
		return null;
	}

	public static Customer getCustomerWithName(DataService service, String name) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from Customer Where DisplayName = '"+name+"'");
		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			Customer customer = (Customer) queryResult.getEntities().get(0);
			return customer;
		}
		return null;
	}

	private static Customer createCustomer(DataService service) throws FMSException, ParseException {
		return service.add(getCustomerWithAllFields());
	}

	public static ReferenceType getCustomerRef(Customer customer) {
		ReferenceType customerRef = new ReferenceType();
		customerRef.setName(customer.getDisplayName());
		customerRef.setValue(customer.getId());
		return customerRef;
	}

	public static Customer getCustomerWithMandatoryFields() throws FMSException {
		Customer customer = new Customer();
		// Mandatory Fields
		customer.setDisplayName(RandomStringUtils.randomAlphanumeric(6));
		return customer;

	}

	public static Customer getCustomerWithAllFields() throws FMSException, ParseException {
		Customer customer = new Customer();
		// Mandatory Fields
		customer.setDisplayName(RandomStringUtils.randomAlphanumeric(6));
		customer.setTitle(RandomStringUtils.randomAlphanumeric(3));
		customer.setGivenName(RandomStringUtils.randomAlphanumeric(6));
		customer.setMiddleName(RandomStringUtils.randomAlphanumeric(6));
		customer.setFamilyName(RandomStringUtils.randomAlphanumeric(6));

		// Optional Fields
		customer.setOrganization(false);
		customer.setSuffix("Sr.");
		customer.setCompanyName("ABC Corporations");
		customer.setPrintOnCheckName("Print name");
		customer.setActive(true);

		customer.setPrimaryPhone(Telephone.getPrimaryPhone());
		customer.setAlternatePhone(Telephone.getAlternatePhone());
		customer.setMobile(Telephone.getMobilePhone());
		customer.setFax(Telephone.getFax());

		customer.setPrimaryEmailAddr(Email.getEmailAddress());

		customer.setContactName("Contact Name");
		customer.setAltContactName("Alternate Name");
		customer.setNotes("Testing Notes");
		customer.setBalance(new BigDecimal("0"));
		customer.setOpenBalanceDate(DateUtils.getCurrentDateTime());
		customer.setBalanceWithJobs(new BigDecimal("5055.5"));
		customer.setCreditLimit(new BigDecimal("200000"));
		customer.setAcctNum("Test020102");
		customer.setResaleNum("40");
		customer.setJob(false);

		customer.setJobInfo(Job.getJobInfo());

//		customer.setBillAddr(Address.getPhysicalAddress());
//		customer.setShipAddr(Address.getPhysicalAddress());

		return customer;

	}

}
