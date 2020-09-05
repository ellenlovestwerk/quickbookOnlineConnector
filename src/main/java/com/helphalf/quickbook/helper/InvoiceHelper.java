package com.helphalf.quickbook.helper;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.helphalf.quickbook.controller.QBOController;
import com.intuit.ipp.data.*;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.serialization.custom.OperationEnumJsonSerializer;
import com.intuit.ipp.services.QueryResult;
import org.apache.commons.lang.RandomStringUtils;

import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.util.DateUtils;
import org.apache.log4j.Logger;
import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

/**
 * @author dderose
 *
 */
public final class InvoiceHelper {

	private static final Logger LOG = Logger.getLogger(QBOController.class);


	private InvoiceHelper() {
		
	}

	public static Invoice createInvoiceFields(DataService service, Map<String,Object> map) throws FMSException, ParseException {


		Invoice invoice = new Invoice();
		
		// Mandatory
		//设置invoice num,默认+1
		//need to find duplicate
//		String sql = "select * from Invoice order by TxnDate DESC";
//		QueryResult queryResult = service.executeQuery(sql);
//
//		Map<String, Object> invoiceMap = JSON.parseObject(invoiceLists, Map.class);
//		List<Map<String, Object>> invoiceList = (List) invoiceMap.get("entities");
//		System.out.println("invoiceList---"+JSON.toJSONString(invoiceList));

//		String docNumber = (String) invoiceList.get(0).get("docNumber");
//		String newDocNumber = addOne(docNumber);
//		System.out.println("type of newDocNumber:"+newDocNumber.getClass());
//		invoice.setDocNumber(newDocNumber);

		//先判断是否给了docNumber-----------------
		String docNumber = (String) map.get("doc_number");
		if (docNumber != null) {
			invoice.setDocNumber(docNumber);
		}

		try {
			invoice.setTxnDate(DateUtils.getCurrentDateTime());
		} catch (ParseException e) {
			throw new FMSException("ParseException while getting current date.");
		}

		//get setting
		Map<String, Object> settingObj = (Map<String, Object>) map.get("settings");
		String accountId = settingObj.get("account_id").toString();
		String taxAgency = settingObj.get("tax_agency_id").toString();
		String itemType = settingObj.get("item_type").toString();
		String itemMatchField = settingObj.get("item_match_field").toString();
		boolean sendEmail = (boolean) settingObj.get("send_email");

		//create new Customer
		Map<String, Object> customerObj = (Map<String, Object>) map.get("customer");
		String customerName = customerObj.get("display_name").toString();
		String customerEmail = customerObj.get("email").toString();
		Customer customer = CustomerHelper.getCustomerWithName(service,customerName);


		if(customer != null ) {
			customer.setId(customer.getId());
		}else {
			customer = getCustomerWithAllFields(service,customerObj);
			Customer savedCustomer = service.add(customer);
			customer = CustomerHelper.getCustomerWithName(service,savedCustomer.getDisplayName());
			if (customerEmail != null) {
				EmailAddress email = Email.getEmailAddressWithAddr(customerEmail);
				customer.setPrimaryEmailAddr(email);
			}
		}
		//		System.out.println("new customer-------"+JSON.toJSONString(customer));
		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));


		//set shipping from address
		Map<String, Object> customerAddressMap = (Map<String, Object>) map.get("customer");
		invoice.setShipAddr(Address.getPhysicalAddress(customerAddressMap));
		invoice.setBillAddr(Address.getPhysicalAddress(customerAddressMap));
		Map<String, Object> shipFromAddr = (Map<String, Object>) map.get("location");
		invoice.setShipFromAddr(Address.getPhysicalAddress(shipFromAddr));

		//get line content
		//添加invoice内line的内容
		List<Line> invLine = new ArrayList<Line>();


		JSONArray lineArray = (JSONArray)map.get("items");

		for(int i = 0; i < lineArray.size(); i++) {
			Line line = new Line();
			String lineDetail = JSON.toJSONString(lineArray.get(i));
			Map<String, Object> lineMap = JSON.parseObject(lineDetail, Map.class);
			String itemName = (String) lineMap.get("name");
			Item item = ItemHelper.getItemWithName(service,itemName);
			String description = (String) lineMap.get("description");
			line.setId(Integer.toString(i+1));

			if(description == null) {
				line.setDescription("");
			}else{
				line.setDescription(description);
			}

			if (item == null ) {
				SalesItemLineDetail silDetails = new SalesItemLineDetail();
				//if it is for shipping
				if (itemName == "shipping") {
					Item savedItem = getItemWithNonInventoryFields(service,lineMap,settingObj);
					silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
					line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
					line.setSalesItemLineDetail(silDetails);
					BigDecimal amount = new BigDecimal(lineMap.get("amount").toString());
					line.setAmount(amount);
					invLine.add(line);
					invoice.setLine(invLine);
				}
				if(itemName == "tax") {
					Item savedItem = getItemWithNonInventoryFields(service,lineMap,settingObj);
					silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
					line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
					line.setSalesItemLineDetail(silDetails);
					BigDecimal amount = new BigDecimal(lineMap.get("amount").toString());

					line.setAmount(amount);
					invLine.add(line);
					invoice.setLine(invLine);
				}

				Item savedItem = getItemWithInventoryFields(service,lineMap,settingObj);
				System.out.println("item detail 01---"+JSON.toJSONString(savedItem));
//				Item newItem = service.add(savedItem);
				System.out.println("item detail 02---"+JSON.toJSONString(savedItem));

//				newItem = ItemHelper.getItemWithName(service,newItem.getName());

				silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
//				Item savedItem = service.add(newItem);
//				savedItem = ItemHelper.getItemWithName(service,itemName);
//				item.setId(savedItem.getId());
//				System.out.println("saved item---------------"+JSON.toJSONString(savedItem));
//				System.out.println("item name---"+savedItem.getName());
//				item = ItemHelper.getItemWithName(service,savedItem.getName());
//				System.out.println("item detail---"+savedItem);
//				savedItem.setId(savedItem.getId());
				BigDecimal qty = new BigDecimal(lineMap.get("quantity").toString());
				silDetails.setQty(qty);
				BigDecimal unitPrice = new BigDecimal(lineMap.get("unit_price").toString());
				silDetails.setUnitPrice(unitPrice);
				BigDecimal tax = new BigDecimal(lineMap.get("tax_amount").toString());
				silDetails.setTaxInclusiveAmt(tax);
				if (!(taxAgency == "" || taxAgency == null || taxAgency.length() == 0)) {
					silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));
				}
				line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
				line.setSalesItemLineDetail(silDetails);

				BigDecimal amount = tax.add(unitPrice.multiply(qty));

				System.out.println("inside amount---"+amount+"------"+tax);

				line.setAmount(amount);
				invLine.add(line);
				invoice.setLine(invLine);
			}else if (item != null){
				SalesItemLineDetail silDetails = new SalesItemLineDetail();

//				System.out.println("item id----"+item.getId());
//				item.setId(item.getId());
				silDetails.setItemRef(ItemHelper.getItemRef(item));
				//			String qty = lineMap.get("quantity").toString();
				BigDecimal qty = new BigDecimal(lineMap.get("quantity").toString());
				silDetails.setQty(qty);
				BigDecimal unitPrice = new BigDecimal(lineMap.get("unit_price").toString());
				silDetails.setUnitPrice(unitPrice);
//				BigDecimal tax = new BigDecimal(lineMap.get("tax_amount").toString());
//				silDetails.setTaxInclusiveAmt(tax);

				if (!(taxAgency == "" || taxAgency == null || taxAgency.length() == 0)) {
					silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));
				}

				line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
				line.setSalesItemLineDetail(silDetails);

				BigDecimal amount = unitPrice.multiply(qty);

//				System.out.println("amount---"+amount+"!!!!!!!!!"+tax);
//			    BigDecimal amount = (BigDecimal) lineMap.get("amount");
				line.setAmount(amount);
				invLine.add(line);
				invoice.setLine(invLine);
			}

		}

		//if choose tax agency, set up sales tax
		System.out.println("taxable-----"+ (!(taxAgency == "" || taxAgency == null || taxAgency.length() == 0)));
		System.out.println("tax agency-----"+taxAgency);

		if (!(taxAgency == "" || taxAgency == null || taxAgency.length() == 0)) {
			TxnTaxDetail txnTaxDetail = new TxnTaxDetail();
			//pass tax code
			TaxCode taxcode = TaxCodeInfo.getTaxCode(service);
			txnTaxDetail.setTxnTaxCodeRef(TaxCodeInfo.getTaxCodeRef(taxcode));
			invoice.setTxnTaxDetail(txnTaxDetail);
		}


		invoice.setPrintStatus(PrintStatusEnum.NEED_TO_PRINT);
//		invoice.setTotalAmt(new BigDecimal("10"));
		invoice.setFinanceCharge(false);

		Invoice savedInvoice = service.add(invoice);

		//send email
		if (sendEmail) {
//			service.sendEmail(invoice, customerEmail);
			System.out.println(sendEmail);
			service.sendEmail(savedInvoice, customer.getPrimaryEmailAddr().getAddress());
		}

		return savedInvoice;
	}

	public static Invoice updateInvoice(DataService service, Map<String,Object> map) throws FMSException, ParseException {

//			DataService service = DataServiceFactory.getDataService();

			//get invoice
			String id = (String) map.get("id");
			Invoice addInvoice= getInvoiceWithId(service,id);
		    System.out.println("invoice-----"+JSON.toJSONString(addInvoice));
		    // sparse update invoice
			addInvoice.setSparse(true);
			List<Line> invLine = new ArrayList<Line>();
			JSONArray lineArray = (JSONArray)map.get("items");
		System.out.println("lineArray----"+lineArray);
			Map<String, Object> settingObj = (Map<String, Object>) map.get("settings");

			for(int i = 0; i < lineArray.size(); i++) {
				Line line = new Line();
				String lineDetail = JSON.toJSONString(lineArray.get(i));
				Map<String, Object> lineMap = JSON.parseObject(lineDetail, Map.class);
				String itemName = (String) lineMap.get("name");
				Item item = ItemHelper.getItemWithName(service,itemName);
				String description = (String) lineMap.get("description");
				line.setId(Integer.toString(i+1));

				if(description == null) {
					line.setDescription("");
				}else{
					line.setDescription(description);
				}

				if (item == null ) {
					SalesItemLineDetail silDetails = new SalesItemLineDetail();
					//if it is for shipping
					if (itemName == "shipping") {
						Item savedItem = getItemWithNonInventoryFields(service,lineMap,settingObj);
						silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
						line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
						line.setSalesItemLineDetail(silDetails);
						BigDecimal amount = new BigDecimal(lineMap.get("amount").toString());
						line.setAmount(amount);
						invLine.add(line);
						addInvoice.setLine(invLine);
					}
					if(itemName == "tax") {
						Item savedItem = getItemWithNonInventoryFields(service,lineMap,settingObj);
						silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
						line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
						line.setSalesItemLineDetail(silDetails);
						BigDecimal amount = new BigDecimal(lineMap.get("amount").toString());

						line.setAmount(amount);
						invLine.add(line);
						addInvoice.setLine(invLine);
					}

					Item savedItem = getItemWithInventoryFields(service,lineMap,settingObj);
					silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
					BigDecimal qty = new BigDecimal(lineMap.get("quantity").toString());
					silDetails.setQty(qty);
					BigDecimal unitPrice = new BigDecimal(lineMap.get("unit_price").toString());
					silDetails.setUnitPrice(unitPrice);
					BigDecimal tax = new BigDecimal(lineMap.get("tax_amount").toString());
					silDetails.setTaxInclusiveAmt(tax);
					silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));

					line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
					line.setSalesItemLineDetail(silDetails);

					BigDecimal amount = tax.add(unitPrice.multiply(qty));

					System.out.println("inside amount---"+amount+"------"+tax);

					line.setAmount(amount);
					invLine.add(line);
					addInvoice.setLine(invLine);
				}else if (item != null){
					SalesItemLineDetail silDetails = new SalesItemLineDetail();

					System.out.println("item id----"+item.getId());

					silDetails.setItemRef(ItemHelper.getItemRef(item));
					BigDecimal qty = new BigDecimal(lineMap.get("quantity").toString());
					silDetails.setQty(qty);
					BigDecimal unitPrice = new BigDecimal(lineMap.get("unit_price").toString());
					silDetails.setUnitPrice(unitPrice);
					BigDecimal tax = new BigDecimal(lineMap.get("tax_amount").toString());
					silDetails.setTaxInclusiveAmt(tax);
					silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));

					line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
					line.setSalesItemLineDetail(silDetails);

					BigDecimal amount = tax.add(unitPrice.multiply(qty));

					System.out.println("amount---"+amount+"!!!!!!!!!"+tax);
					line.setAmount(amount);
					invLine.add(line);
					addInvoice.setLine(invLine);
				}

			}

			Invoice savedInvoice = service.update(addInvoice);
			LOG.info("Invoice sparse updated: " + savedInvoice.getId() + " doc num ::: " + savedInvoice.getDocNumber() );

			return savedInvoice;

		}


	
	public static Invoice getASTInvoiceFields(DataService service) throws FMSException, ParseException {
		Invoice invoice = new Invoice();
		
		//add customer
		Customer customer = CustomerHelper.getCustomer(service);
		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));

		// add line
		List<Line> invLine = new ArrayList<Line>();
		Line line = new Line();
		line.setAmount(new BigDecimal("100"));
		line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
			
		SalesItemLineDetail silDetails = new SalesItemLineDetail();
		
		Item item = ItemHelper.getItem(service);
		silDetails.setItemRef(ItemHelper.getItemRef(item));
		
		//set line item as taxable
		silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));

		line.setSalesItemLineDetail(silDetails);
		invLine.add(line);
		invoice.setLine(invLine);
		
		TxnTaxDetail txnTaxDetail = new TxnTaxDetail();


		//pass dummy tax code
		TaxCode taxcode = TaxCodeInfo.getTaxCode(service);
		txnTaxDetail.setTxnTaxCodeRef(TaxCodeInfo.getTaxCodeRef(taxcode));
		invoice.setTxnTaxDetail(txnTaxDetail);
		
		//set shipping address
		invoice.setShipAddr(Address.getAddressForAST());

		return invoice;
	}
	
	public static Invoice getASTOverrideFields(DataService service) throws FMSException, ParseException {
		Invoice invoice = new Invoice();
		
		//add customer
		Customer customer = CustomerHelper.getCustomer(service);
		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));

		// add line
		List<Line> invLine = new ArrayList<Line>();
		Line line = new Line();
		line.setAmount(new BigDecimal("100"));
		line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
			
		SalesItemLineDetail silDetails = new SalesItemLineDetail();
		
		Item item = ItemHelper.getItem(service);
		silDetails.setItemRef(ItemHelper.getItemRef(item));
		
		//set line item as taxable
		silDetails.setTaxCodeRef(TaxCodeInfo.getTaxCodeRef("TAX"));

		line.setSalesItemLineDetail(silDetails);
		invLine.add(line);
		invoice.setLine(invLine);
		
		TxnTaxDetail txnTaxDetail = new TxnTaxDetail();
		//override tax value
		txnTaxDetail.setTotalTax(new BigDecimal("12"));
		invoice.setTxnTaxDetail(txnTaxDetail);
		
		//set shipping address
		invoice.setShipAddr(Address.getAddressForAST());

		return invoice;
	}

	public static String addOne(String Str) {
		String[] strs = Str.split("[^0-9]");//根据不是数字的字符拆分字符串
		String numStr = strs[strs.length - 1];//取出最后一组数字
		if (numStr != null && numStr.length() > 0) {//如果最后一组没有数字(也就是不以数字结尾)，抛NumberFormatException异常
			int n = numStr.length();//取出字符串的长度
			int num = Integer.parseInt(numStr) + 1;//将该数字加一
			String added = String.valueOf(num);
			n = Math.min(n, added.length());
			//拼接字符串
			return Str.subSequence(0, Str.length() - n) + added;
		} else {
			throw new NumberFormatException();
		}
	}

	private static Customer getCustomerWithAllFields( DataService service,Map customerObj) throws FMSException {
		Customer customer = new Customer();
		customer.setDisplayName(customerObj.get("display_name").toString());
		customer.setContactName(customerObj.get("name").toString());
		TelephoneNumber primaryNum = new TelephoneNumber();
		primaryNum.setFreeFormNumber(customerObj.get("phone").toString());
		customer.setPrimaryPhone(primaryNum);

		customer.setPrimaryEmailAddr(Email.getEmailAddress());

		EmailAddress emailAddr = new EmailAddress();
		emailAddr.setAddress(customerObj.get("email").toString());
		customer.setPrimaryEmailAddr(emailAddr);

		PhysicalAddress customerShipAddr = new PhysicalAddress();
		customerShipAddr.setLine1(customerObj.get("street1").toString());
		customerShipAddr.setLine2(customerObj.get("street2").toString());
		customerShipAddr.setCity(customerObj.get("city").toString());
		customerShipAddr.setCountrySubDivisionCode(customerObj.get("state").toString());
		customerShipAddr.setPostalCode(customerObj.get("zip_code").toString());
		customerShipAddr.setCountry(customerObj.get("country").toString());

		customer.setShipAddr(customerShipAddr);
//		Customer savedCustomer = service.add(customer);

//		service.add(customer);
//		System.out.println("customer22----"+JSON.toJSONString(customer));
		return customer;
	}

	public static Item getItemWithInventoryFields(DataService service, Map itemObj, Map settingObj) throws FMSException, ParseException {

		Item item = new Item();
		System.out.println("itemObj---"+ JSON.toJSONString(itemObj));
		item.setName(itemObj.get("name").toString());
		String incomeAccountName = settingObj.get("income_account").toString();
		String assetAccountName = settingObj.get("asset_account").toString();

//		item.setActive(true);
//		item.setTaxable(false);
		item.setUnitPrice(new BigDecimal(itemObj.get("unit_price").toString()) );
		item.setType(ItemTypeEnum.INVENTORY);
		item.setTrackQtyOnHand(true);
		item.setQtyOnHand(BigDecimal.valueOf(0));
		item.setInvStartDate(DateUtils.getCurrentDateTime());
/*
for item
{
  "TrackQtyOnHand": true,
  "Name": "Garden Supplies",
  "QtyOnHand": 0,
  "IncomeAccountRef": {
    "name": "Sales of Product Income",
    "value": "79"
  },
  "AssetAccountRef": {
    "name": "Inventory Asset",
    "value": "81"
  },
  "InvStartDate": "2015-01-01",
  "Type": "Inventory",
  "ExpenseAccountRef": {
    "name": "Cost of Goods Sold",
    "value": "80"
  }
}
*/


/*
for service
{

}
*/
//		Account expenseAccount = AccountHelper.getExpenseBankAccount(service);
		Account expenseAccount =AccountHelper.getAccountWithName(service,"Cost of Goods Sold");
		item.setExpenseAccountRef(AccountHelper.getExpenseAccountRef(expenseAccount));

		Account assetAccount = AccountHelper.getAccountWithName(service,"Inventory Asset");
		item.setAssetAccountRef(AccountHelper.getAssetAccountRef(assetAccount));

		Account incomeAccount = AccountHelper.getAccountWithName(service,"Sales of Product Income");
		item.setIncomeAccountRef(AccountHelper.getIncomeAccountRef(incomeAccount));


//		System.out.println("saved item---"+JSON.toJSONString(item));
		service.add(item);
//		System.out.println("saved item---"+JSON.toJSONString(item));

//		return savedItem;
		return item;
	}

	public static Item getItemWithNonInventoryFields(DataService service, Map itemObj, Map settingObj) throws FMSException, ParseException {

		Item item = new Item();
		System.out.println("itemObj---"+ JSON.toJSONString(itemObj));
		item.setName(itemObj.get("name").toString());

		String incomeAccountName = settingObj.get("income_account").toString();

		item.setType(ItemTypeEnum.SERVICE);

		Account expenseAccount =AccountHelper.getAccountWithName(service,"Cost of Goods Sold");
		item.setExpenseAccountRef(AccountHelper.getExpenseAccountRef(expenseAccount));

		Account incomeAccount = AccountHelper.getAccountWithName(service,incomeAccountName);
		item.setIncomeAccountRef(AccountHelper.getIncomeAccountRef(incomeAccount));

		service.add(item);
		return item;
	}


//	  public static Invoice getInvoice(DataService service) throws FMSException, ParseException {
//			List<Invoice> invoices = (List<Invoice>) service.findAll(new Invoice());
//			if (!invoices.isEmpty()) {
//				return invoices.get(0);
//			}
//			return createItem(service);
//	  }
//
//
//	private static Invoice createItem(DataService service) throws FMSException, ParseException {
//		return service.add(getInvoiceFields(service));
//
//	}
//
//	public static ReferenceType getInvoiceRef(Invoice invoice) {
//			ReferenceType invoiceRef = new ReferenceType();
//			invoiceRef.setValue(invoice.getId());
//			return invoiceRef;
//	}

	public static Invoice getInvoiceWithId(DataService service, String id) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from Invoice Where id = '"+id+"'");

		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			Invoice invoice = (Invoice) queryResult.getEntities().get(0);
			System.out.println("invoice------"+invoice);
			return invoice;
		}
		return null;
	}
	
	  
}
