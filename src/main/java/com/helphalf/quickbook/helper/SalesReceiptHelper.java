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
public final class SalesReceiptHelper {

	private SalesReceiptHelper() {

	}
	private static final Logger LOG = Logger.getLogger(QBOController.class);

	public static SalesReceipt getSalesReceiptFields(DataService service, Map<String,Object> map) throws FMSException, ParseException {


		SalesReceipt salesReceipt = new SalesReceipt();


		//先判断是否给了docNumber-----------------
		String docNumber = (String) map.get("doc_number");
		if (docNumber != null) {
			salesReceipt.setDocNumber(docNumber);
		}

		try {
			salesReceipt.setTxnDate(DateUtils.getCurrentDateTime());
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
		System.out.println("new customer-------"+JSON.toJSONString(customer));
		salesReceipt.setCustomerRef(CustomerHelper.getCustomerRef(customer));


		//set shipping from address
		Map<String, Object> customerAddressMap = (Map<String, Object>) map.get("customer");
		salesReceipt.setShipAddr(Address.getPhysicalAddress(customerAddressMap));
		salesReceipt.setBillAddr(Address.getPhysicalAddress(customerAddressMap));
		Map<String, Object> shipFromAddr = (Map<String, Object>) map.get("location");
		salesReceipt.setShipFromAddr(Address.getPhysicalAddress(shipFromAddr));

		//get line content
		//添加SalesReceipt内line的内容
		List<Line> invLine = new ArrayList<Line>();
//		Line line = new Line();

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
				Item savedItem = getItemWithFields(service,lineMap);
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


				line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
				line.setSalesItemLineDetail(silDetails);

				BigDecimal amount = tax.add(unitPrice.multiply(qty));

//				System.out.println("inside amount---"+amount+"------"+tax);

				line.setAmount(amount);
				invLine.add(line);
				salesReceipt.setLine(invLine);
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
				BigDecimal tax = new BigDecimal(lineMap.get("tax_amount").toString());
				silDetails.setTaxInclusiveAmt(tax);

				line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
				line.setSalesItemLineDetail(silDetails);

				BigDecimal amount = tax.add(unitPrice.multiply(qty));
				System.out.println("amount---"+amount+"!!!!!!!!!"+tax);
//			BigDecimal amount = (BigDecimal) lineMap.get("amount");
				line.setAmount(amount);
				invLine.add(line);
				salesReceipt.setLine(invLine);
			}

		}


		salesReceipt.setPrintStatus(PrintStatusEnum.NEED_TO_PRINT);
		salesReceipt.setFinanceCharge(false);

		SalesReceipt savedSalesReceipt = service.add(salesReceipt);

		//send email
//		System.out.println("email------"+customer.getPrimaryEmailAddr().getAddress()); //can get the email address
		System.out.println("send email---"+ sendEmail);

		if (sendEmail) {
			System.out.println(sendEmail);
			service.sendEmail(savedSalesReceipt, customer.getPrimaryEmailAddr().getAddress());

		}

		return savedSalesReceipt;
	}

	public static SalesReceipt updateSalesReceipt(DataService service, Map<String,Object> map) throws FMSException, ParseException {

//			DataService service = DataServiceFactory.getDataService();

		//get SalesReceipt
		String id = (String) map.get("id");
		SalesReceipt addSalesReceipt= getSalesReceiptWithId(service,id);
		System.out.println("invoice-----"+JSON.toJSONString(addSalesReceipt));
		// sparse update SalesReceipt
		addSalesReceipt.setSparse(true);
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
					addSalesReceipt.setLine(invLine);
				}
				if(itemName == "tax") {
					Item savedItem = getItemWithNonInventoryFields(service,lineMap,settingObj);
					silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
					line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
					line.setSalesItemLineDetail(silDetails);
					BigDecimal amount = new BigDecimal(lineMap.get("amount").toString());

					line.setAmount(amount);
					invLine.add(line);
					addSalesReceipt.setLine(invLine);
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
				addSalesReceipt.setLine(invLine);
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
				addSalesReceipt.setLine(invLine);
			}

		}
		SalesReceipt savedSalesReceipt = service.update(addSalesReceipt);
		LOG.info("Invoice sparse updated: " + savedSalesReceipt.getId() + " doc num ::: " + savedSalesReceipt.getDocNumber() );

		return savedSalesReceipt;
	}

	public static SalesReceipt getSalesReceiptWithId(DataService service, String id) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from SalesReceipt Where id = '"+id+"'");

		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			SalesReceipt salesReceipt = (SalesReceipt) queryResult.getEntities().get(0);
			System.out.println("SalesReceipt------"+salesReceipt);
			return salesReceipt;
		}
		return null;
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

	public static Item getItemWithFields(DataService service, Map itemObj) throws FMSException, ParseException {

		Item item = new Item();
		System.out.println("itemObj---"+ JSON.toJSONString(itemObj));
		item.setName(itemObj.get("name").toString());
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


		service.add(item);
		return item;
	}




}
