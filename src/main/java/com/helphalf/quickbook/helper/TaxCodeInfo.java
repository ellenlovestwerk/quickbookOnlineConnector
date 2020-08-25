package com.helphalf.quickbook.helper;

import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.data.TaxCode;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;

import java.util.List;

/**
 * @author dderose
 *
 */
public final class TaxCodeInfo {
	
	private TaxCodeInfo() {
		
	}

	public static TaxCode getTaxCode(DataService service) throws FMSException {
		List<TaxCode> taxcodes = (List<TaxCode>) service.findAll(new TaxCode());
		return taxcodes.get(0); 
	}
	
	  public static ReferenceType getTaxCodeRef(TaxCode taxcode) {
			ReferenceType taxcodeRef = new ReferenceType();
			taxcodeRef.setName(taxcode.getName());
			taxcodeRef.setValue(taxcode.getId());
			return taxcodeRef;
	  }
	  
	  public static ReferenceType getTaxCodeRef(String taxcode) {
			ReferenceType taxcodeRef = new ReferenceType();
			taxcodeRef.setValue(taxcode);
			return taxcodeRef;
	  }

}
