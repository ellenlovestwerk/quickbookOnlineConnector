package com.helphalf.quickbook.helper;

import com.alibaba.fastjson.JSON;
import com.intuit.ipp.data.PhysicalAddress;
import com.intuit.ipp.data.WebSiteAddress;

import java.util.Map;

/**
 * @author dderose
 *
 */
public final class Address {
	
	private Address() {
		
	}

	public static PhysicalAddress getPhysicalAddress(Map customerAddr) {
		PhysicalAddress address = new PhysicalAddress();
		address.setLine1(customerAddr.get("street1").toString());
		address.setCity(customerAddr.get("city").toString());
		address.setPostalCode(customerAddr.get("zip_code").toString());
		address.setCountrySubDivisionCode(customerAddr.get("state").toString());
//		System.out.println("address in getPhysicalAdress----"+customerAddr.toString());

//		billingAdd.setLine1();
//		billingAdd.setCity("Mountain View");
//		billingAdd.setCountry("United States");
//		billingAdd.setCountrySubDivisionCode("CA");
//		billingAdd.setPostalCode("94043");

		return address;
	}
//	public static PhysicalAddress getPhysicalAddress(Map addr) {
//		PhysicalAddress address = new PhysicalAddress();
//		address.setLine1(addr.get("street1").toString());
//		address.setCity(addr.get("city").toString());
//		address.setPostalCode(addr.get("zip_code").toString());
//		address.setCountrySubDivisionCode(addr.get("state").toString());

//		billingAdd.setLine1();
//		billingAdd.setCity("Mountain View");
//		billingAdd.setCountry("United States");
//		billingAdd.setCountrySubDivisionCode("CA");
//		billingAdd.setPostalCode("94043");
//
//		return address;
//	}

//	public static PhysicalAddress setPhysicalAddress(String add) {
//		PhysicalAddress shippingAdd = new PhysicalAddress();
//		shippingAdd.setLine1();
//		shippingAdd.setCity();
//		shippingAdd.setCountry();
//		shippingAdd.setCountry("United States");
//		shippingAdd.setCountrySubDivisionCode();
//		shippingAdd.setPostalCode();
//		return shippingAdd;
//	}
	
	public static WebSiteAddress getWebSiteAddress() {
		WebSiteAddress webSite = new WebSiteAddress();
		webSite.setURI("http://abccorp.com");
		webSite.setDefault(true);
		webSite.setTag("Business");
		return webSite;
	}
	
	public static PhysicalAddress getAddressForAST() {
		PhysicalAddress billingAdd = new PhysicalAddress();
		billingAdd.setLine1("2700 Coast Ave");
		billingAdd.setLine2("MountainView, CA 94043");
		return billingAdd;
	}

}
