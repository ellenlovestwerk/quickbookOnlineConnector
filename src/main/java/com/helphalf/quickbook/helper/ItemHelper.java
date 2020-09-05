package com.helphalf.quickbook.helper;

import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import org.apache.commons.lang.RandomStringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * @author dderose
 *
 */
public final class ItemHelper {

	private ItemHelper() {
		
	}

	public static Item getItemFields(DataService service) throws FMSException {

		Item item = new Item();
		item.setName("Item" + RandomStringUtils.randomAlphanumeric(5));
		item.setActive(true);
		item.setTaxable(false);
		item.setUnitPrice(new BigDecimal("200"));
		item.setType(ItemTypeEnum.SERVICE);

//		Account incomeAccount = AccountHelper.getAccount(service);
//		item.setIncomeAccountRef(AccountHelper.getIncomeBankAccount(incomeAccount));


//		Account expenseAccount = AccountHelper.getExpenseBankAccount(service);
//		item.setExpenseAccountRef(AccountHelper.getAccountRef(expenseAccount));
		Account incomeAccount = AccountHelper.getIncomeBankAccount(service);
		item.setIncomeAccountRef(AccountHelper.getIncomeAccountRef(incomeAccount));
//		item.setPurchaseCost((BigDecimal) itemObj.get("purchase_price"));
		item.setPurchaseCost(new BigDecimal("300"));
		Account expenseAccount = AccountHelper.getExpenseBankAccount(service);
		item.setExpenseAccountRef(AccountHelper.getExpenseAccountRef(incomeAccount));

		Account assetAccount = AccountHelper.getAssetAccount(service);
		item.setAssetAccountRef(AccountHelper.getAssetAccountRef(assetAccount));

		item.setTrackQtyOnHand(false);

		return item;
	}


	public static Item getItem(DataService service) throws FMSException {
		List<Item> items = service.findAll(new Item());
		if (!items.isEmpty()) { 
			return items.get(0); 
		}
		return createItem(service);
	}

	public static Item getItemWithId(DataService service, String id) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from Item Where id = '"+id+"'");
		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			Item item = (Item) queryResult.getEntities().get(0);
			return item;
		}
		return null;
	}
	public static Item getItemWithName(DataService service, String name) throws FMSException, ParseException {
		QueryResult queryResult = service.executeQuery("select * from Item Where Name = '"+name+"'");

		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			Item item = (Item) queryResult.getEntities().get(0);
//			System.out.println("item------"+item);
			return item;
		}
		return null;
	}


	private static Item createItem(DataService service) throws FMSException {
		return service.add(getItemFields(service));
	}

	public static ReferenceType getItemRef(Item item) {
		ReferenceType itemRef = new ReferenceType();
		itemRef.setName(item.getName());
		itemRef.setValue(item.getId());
		return itemRef;
	}

}
