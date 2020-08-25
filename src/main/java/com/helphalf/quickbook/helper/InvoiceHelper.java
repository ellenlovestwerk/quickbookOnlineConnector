package com.helphalf.quickbook.helper;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intuit.ipp.data.*;
import com.intuit.ipp.serialization.custom.OperationEnumJsonSerializer;
import com.intuit.ipp.services.QueryResult;
import org.apache.commons.lang.RandomStringUtils;

import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.util.DateUtils;
import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

/**
 * @author dderose
 *
 */
public final class InvoiceHelper {
	
	private InvoiceHelper() {
		
	}

	public static Invoice getInvoiceFields(DataService service, Map<String,Object> map) throws FMSException, ParseException {


		Invoice invoice = new Invoice();
		
		// Mandatory
		//设置invoice num，需要找到quickbook的默认方式！！！！！！！！！！！！！！！！！！！！！！！！！
		String sql = "select * from Invoice order by TxnDate DESC";
		QueryResult queryResult = service.executeQuery(sql);

		String invoiceLists = JSON.toJSONString(queryResult);
		Map<String, Object> invoiceMap = JSON.parseObject(invoiceLists, Map.class);

		List<Map<String, Object>> invoiceList = (List) invoiceMap.get("entities");

		//get the latest docNumber
		String docNumber = (String) invoiceList.get(0).get("docNumber");

		//call addOne to add 1 to the last docNumber
		String newDocNumber = addOne(docNumber);
//		System.out.println("type of newDocNumber:"+newDocNumber.getClass());
		invoice.setDocNumber(newDocNumber);

		try {
			invoice.setTxnDate(DateUtils.getCurrentDateTime());
		} catch (ParseException e) {
			throw new FMSException("ParseException while getting current date.");
		}

		//create new Customer
		Map<String, Object> customerObj = (Map<String, Object>) map.get("customer");
		String customerName = customerObj.get("display_name").toString();

		Customer customer = CustomerHelper.getCustomerWithName(service,customerName);


		if(customer != null ) {
			customer.setId(customer.getId());
		}else {
			customer = getCustomerWithAllFields(service,customerObj);
			Customer savedCustomer = service.add(customer);
			customer = CustomerHelper.getCustomerWithName(service,savedCustomer.getDisplayName());
		}
		System.out.println("new customer-------"+JSON.toJSONString(customer));
		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));

//		System.out.println("type----------------"+customerObj.getClass());
//		Customer customer = new Customer();
//		customer.setDisplayName(customerObj.get("display_name").toString());
//		customer.setContactName(customerObj.get("name").toString());
//		TelephoneNumber primaryNum = new TelephoneNumber();
//		primaryNum.setFreeFormNumber(customerObj.get("phone").toString());
//		customer.setPrimaryPhone(Telephone.getPrimaryPhone());
//		EmailAddress emailAddr = new EmailAddress();
//		emailAddr.setAddress(customerObj.get("email").toString());
//		customer.setPrimaryEmailAddr(Email.getEmailAddress());

//		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));
//		System.out.println(customer.toString());
		//拿到customer的info
//		Map<String, Object> customerMap = (Map<String, Object>) map.get("CustomerRef");
		//拿到customer在quickbook中的id
//		String customerId = (String) customerMap.get("value");

		//用sql拿到database中this.id的customer
//		Customer customer = CustomerHelper.getCustomerWithId(service,customerId);

		//为什么需要这步？
//		customer.setId(customer.getId());

		//invoice中插入this.id的customer
//		invoice.setCustomerRef(CustomerHelper.getCustomerRef(customer));

//这个是什么？ setBalance
//		invoice.setBalance(new BigDecimal("10000"));

		//set shipping from address
		Map<String, Object> customerAddressMap = (Map<String, Object>) map.get("customer");
		invoice.setShipAddr(Address.getPhysicalAddress(customerAddressMap));
		invoice.setBillAddr(Address.getPhysicalAddress(customerAddressMap));
		Map<String, Object> shipFromAddr = (Map<String, Object>) map.get("location");
		invoice.setShipFromAddr(Address.getPhysicalAddress(shipFromAddr));

		//get line content
		//添加invoice内line的内容
		List<Line> invLine = new ArrayList<Line>();
//		Line line = new Line();

		JSONArray lineArray = (JSONArray)map.get("items");
		//lineList是ArrayList type, 为什么还需要JSON.toJSONString()???????
		System.out.println("lineArray-------------"+lineArray);
//		List<Line> lineList = JSON.parseArray(lineArray.toString(), Line.class);
//		System.out.println("lineList-------------"+lineList);

		for(int i = 0; i < lineArray.size(); i++) {
			Line line = new Line();
			String lineDetail = JSON.toJSONString(lineArray.get(i));
			Map<String, Object> lineMap = JSON.parseObject(lineDetail, Map.class);
		    System.out.println("lineMap---------"+lineMap);
			String itemName = (String) lineMap.get("name");
			Item item = ItemHelper.getItemWithName(service,itemName);
			System.out.println("item--"+JSON.toJSONString(item));
			String description = (String) lineMap.get("description");
			line.setId(Integer.toString(i+1));

			if(description == null) {
				line.setDescription("");
			}else{
				line.setDescription(description);
			}
//			JSONObject saleDetail = (JSONObject)lineMap.get("salesItemLineDetail");
//			Map<String, Object> salesItemLineDetailMap = JSONObject.toJavaObject(saleDetail, Map.class);
//			System.out.println("itemname---------"+lineMap.get("name"));

			if (item == null ) {
//				Line line02 = new Line();
//				line02.setDescription("New test (14.48mm)\nGiftBox: Red Heart Gift Box [ £ 3.00]");
//				line02.setAmount(new BigDecimal("10000"));
//				line02.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
//				Item item02 = ItemHelper.addItemWithFields(service,items);
//				System.out.println("item returned---"+ JSON.toJSONString(items));
//				silDetails02.setItemRef(ItemHelper.getItemRef(item02));
//				line02.setSalesItemLineDetail(silDetails02);
//				invLine.add(line02);
//				invoice.setLine(invLine);
				SalesItemLineDetail silDetails = new SalesItemLineDetail();
				Item savedItem = getItemWithFields(service,lineMap);
				System.out.println("item detail 01---"+JSON.toJSONString(savedItem));
				silDetails.setItemRef(ItemHelper.getItemRef(savedItem));
//				Item savedItem = service.add(newItem);
//				savedItem = ItemHelper.getItemWithName(service,itemName);
//				item.setId(savedItem.getId());
				System.out.println("saved item---------------"+JSON.toJSONString(savedItem));
//				System.out.println("item name---"+savedItem.getName());
//				item = ItemHelper.getItemWithName(service,savedItem.getName());
				System.out.println("item detail---"+savedItem);
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
//			BigDecimal amount = (BigDecimal) lineMap.get("amount");
				System.out.println("inside amount---"+amount+"------"+tax);

				line.setAmount(amount);
				invLine.add(line);
				invoice.setLine(invLine);
			}else if (item != null){
				SalesItemLineDetail silDetails = new SalesItemLineDetail();

				System.out.println("item id----"+item.getId());
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
				invoice.setLine(invLine);
			}

		}


		invoice.setPrintStatus(PrintStatusEnum.NEED_TO_PRINT);
//		invoice.setTotalAmt(new BigDecimal("10"));
		invoice.setFinanceCharge(false);

		//send email
//		System.out.println("email------"+customer.getPrimaryEmailAddr().getAddress()); //can get the email address
//		service.sendEmail(invoice, customer.getPrimaryEmailAddr().getAddress());

		return invoice;
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

	public static Item getItemWithFields(DataService service, Map itemObj) throws FMSException {

		Item item = new Item();
		System.out.println("itemObj---"+ JSON.toJSONString(itemObj));
		item.setName(itemObj.get("name").toString());
		item.setActive(true);
		item.setTaxable(false);
		item.setUnitPrice(new BigDecimal(itemObj.get("unit_price").toString()) );
		item.setType(ItemTypeEnum.INVENTORY);


		Account incomeAccount = AccountHelper.getIncomeBankAccount(service);
		item.setIncomeAccountRef(AccountHelper.getAccountRef(incomeAccount));
//		item.setPurchaseCost((BigDecimal) itemObj.get("purchase_price"));

		Account expenseAccount = AccountHelper.getExpenseBankAccount(service);
		item.setExpenseAccountRef(AccountHelper.getAccountRef(expenseAccount));

		Account assetAccount = AccountHelper.getAssetAccount(service);
		item.setAssetAccountRef(AccountHelper.getAccountRef(assetAccount));


		item.setTrackQtyOnHand(true);
		item.setQtyOnHand(BigDecimal.valueOf(0));
		System.out.println("saved item---"+JSON.toJSONString(item));
		Item savedItem = service.add(item);
		System.out.println("return----");
		return savedItem;
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
	
	
	  
}
