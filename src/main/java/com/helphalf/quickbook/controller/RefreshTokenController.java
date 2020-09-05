package com.helphalf.quickbook.controller;

import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dderose
 *
 */
@Controller
public class RefreshTokenController {
	
	@Autowired
	OAuth2PlatformClientFactory factory;
	
	private static final Logger logger = Logger.getLogger(RefreshTokenController.class);
	
    /**
     * Call to refresh tokens 
     * 
     * @param
     * @return
     */
	@ResponseBody
    @RequestMapping("/refreshToken")
    public ResponseEntity refreshToken(@RequestParam("refreshToken") String refreshToken) {
 
        try {
        	
        	OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
        	BearerTokenResponse bearerTokenResponse = client.refreshToken(refreshToken);

//				工程中需要用的
				String auth = new JSONObject()
						.put("access_token", bearerTokenResponse.getAccessToken())
						.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();

            return ResponseEntity.ok(auth);
        }

        catch (OAuthException ex) {
            logger.error("OAuthException while calling refreshToken ", ex);
            logger.error("intuit_tid: " + ex.getIntuit_tid());
            logger.error("More info: " + ex.getResponseContent());

            Map<String, Object> er = new HashMap<>();
                er.put("response status:", "404");
                er.put("message", "Third party quickbook error: Can not get refresh token!");
            return ResponseEntity.badRequest().body(er);
        }
        catch (Exception ex) {
        	logger.error("Exception while calling refreshToken ", ex);

            Map<String, Object> er = new HashMap<>();
            er.put("response status:", "404");
            er.put("message", "Third party quickbook error: Can not get refresh token!");
            return ResponseEntity.badRequest().body(er);
        }

    }

}
