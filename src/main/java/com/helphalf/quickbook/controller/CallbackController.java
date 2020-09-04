package com.helphalf.quickbook.controller;

import com.alibaba.fastjson.JSON;
import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.data.UserInfoResponse;
import com.intuit.oauth2.exception.OAuthException;
import com.intuit.oauth2.exception.OpenIdException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;


@Controller
public class CallbackController {
    
	@Autowired
	OAuth2PlatformClientFactory factory;

    private static final Logger logger = Logger.getLogger(CallbackController.class);
    
    /**
     *  This is the redirect handler you configure in your app on developer.intuit.com
     *  The Authorization code has a short lifetime.
     *  Hence Unless a user action is quick and mandatory, proceed to exchange the Authorization Code for
     *  BearerToken
	 *
     * @param authCode
     * @param state
     * @param realmId

     * @return
     */
    @RequestMapping("/oauth2redirect")
	@ResponseBody
    public String callBackFromOAuth(@RequestParam("code") String authCode, @RequestParam("state") String state, @RequestParam(value = "realmId", required = false) String realmId, HttpSession session) {
        logger.info("inside oauth2redirect of sample"  );

        try {
	        String csrfToken = (String) session.getAttribute("csrfToken");
	        if (csrfToken.equals(state)) {
//	            session.setAttribute("realmId", realmId);
//	            session.setAttribute("auth_code", authCode);
	
	            OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
	            String redirectUri = factory.getPropertyValue("OAuth2AppRedirectUri");
	            logger.info("inside oauth2redirect of sample -- redirectUri " + redirectUri  );
	            BearerTokenResponse bearerTokenResponse = client.retrieveBearerTokens(authCode, redirectUri);
				 
//	            session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
//	            session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());

//
				ContextFactory.bearerToken = bearerTokenResponse.getAccessToken();
				ContextFactory.refreshToken = bearerTokenResponse.getRefreshToken();
				ContextFactory.companyID = realmId;


//
//				String bearerToken = bearerTokenResponse.getAccessToken();
//				String refreshToken = bearerTokenResponse.getRefreshToken();
//				Map<String, Object> auth = new HashMap<String, Object>();
//				auth.put("access_token", bearerToken);
//				auth.put("refresh_token", refreshToken);
//				auth.put("realm_id", realmId);



//				String jsonString = new JSONObject()
//						.put("realm_id",realmId)
//						.put("access_token", bearerTokenResponse.getAccessToken())
//						.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();



	            /*
	                Update your Data store here with user's AccessToken and RefreshToken along with the realmId
	    
	                However, in case of OpenIdConnect, when you request OpenIdScopes during authorization,
	                you will also receive IDToken from Intuit.
	                You first need to validate that the IDToken actually came from Intuit.
	             */
	    
	            if (StringUtils.isNotBlank(bearerTokenResponse.getIdToken())) {
	               try {
						if(client.validateIDToken(bearerTokenResponse.getIdToken())) {
						       logger.info("IdToken is Valid");
						       //get user info
						       saveUserInfo(bearerTokenResponse.getAccessToken(), session, client);
						   }
					} catch (OpenIdException e) {
						logger.error("Exception validating id token ", e);
					   logger.error("intuit_tid: " + e.getIntuit_tid());
					   logger.error("More info: " + e.getResponseContent());
					}
	            }
	            
	            return "connected";

//				System.out.println("auth type"+ auth.getClass());

//				return JSON.toJSONString(auth);



			}
	        logger.info("csrf token mismatch " );
        } catch (OAuthException e) {
        	logger.error("Exception in callback handler ", e);
		} 
        return null;
    }

    
    private void saveUserInfo(String accessToken, HttpSession session, OAuth2PlatformClient client) {
        //Ideally you would fetch the realmId and the accessToken from the data store based on the user account here.
 
        try {
        	UserInfoResponse response = client.getUserInfo(accessToken); 

        	session.setAttribute("sub", response.getSub());
            session.setAttribute("givenName", response.getGivenName());
            session.setAttribute("email", response.getEmail());
            
        }
        catch (Exception ex) {
            logger.error("Exception while retrieving user info ", ex);
        }
    }

}
