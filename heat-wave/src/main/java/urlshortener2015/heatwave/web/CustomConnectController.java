package urlshortener2015.heatwave.web;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.view.RedirectView;

import urlshortener2015.heatwave.entities.ShortURL;
import urlshortener2015.heatwave.repository.ShortURLRepository;
import urlshortener2015.heatwave.utils.ShortURLUtils;

/**
 * Overrides default ConnectController
 */
@Controller
@RequestMapping("/connect")
public class CustomConnectController extends ConnectController {

	private static final Logger logger = LoggerFactory.getLogger(CustomConnectController.class);
	
	@Autowired
	private Facebook facebook;
	
	@Autowired
	private Twitter twitter;
	
	@Autowired
	private ShortURLRepository shortURLRepository;
	
    @Inject
    public CustomConnectController(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository) {
        super(connectionFactoryLocator, connectionRepository);
    }
    
    @RequestMapping(value = "/{providerId:facebook|twitter}/{id:(?!link|!stadistics|index).*}", method = RequestMethod.POST)
    protected RedirectView connect(@PathVariable("providerId") String providerId, @PathVariable("id") String id, NativeWebRequest request){
    	logger.info("POST TO /connect/"+providerId+"/"+id);
    	return super.connect(providerId, request);
    }
    
    @RequestMapping(value = "/{providerId:facebook|twitter}/{id:(?!link|!stadistics|index).*}", method = RequestMethod.GET, params="oauth_token")
    protected String OAuth1Callback(@PathVariable("providerId") String providerId, @PathVariable("id") String id, NativeWebRequest request, Model model){
    	logger.info("GET TO /connect/"+providerId+"/"+id+" (OAuth1Callback)");
    	super.oauth1Callback(providerId, request);
    	//super.connectionStatus(providerId, request, model);
    	ShortURL url = shortURLRepository.findByHash(id);
    	model.addAttribute("targetURL", url.getTarget());
		model.addAttribute("countDown", MainController.DEFAULT_COUNTDOWN);
		model.addAttribute("advertisement", MainController.DEFAULT_AD_PATH);
    	if (url.getAds()){
    		logger.info("t: " + twitter == null ? "null" : "not null");
    		logger.info("f: " + facebook == null ? "null" : "not null");
    		switch(providerId){
	    		case "facebook":
	    			if (facebook.isAuthorized()) model.addAttribute("enableAds", !ShortURLUtils.isUserInList(url, facebook));
	    			else model.addAttribute("enableAds", true);
	    			break;
	    		case "twitter":
	    			if (twitter.isAuthorized()) model.addAttribute("enableAds", !ShortURLUtils.isUserInList(url, twitter));
	    			else model.addAttribute("enableAds", true);
	    			break;
    			default:
    				break;
    		}
    	}
    	else model.addAttribute("enableAds", false);
    	return connectedView(providerId, id);
    }
    
    @RequestMapping(value = "/{providerId:facebook|twitter}/{id:(?!link|!stadistics|index).*}", method = RequestMethod.GET, params="code")
    protected String OAuth2Callback(@PathVariable("providerId") String providerId, @PathVariable("id") String id, NativeWebRequest request, Model model){
    	logger.info("GET TO /connect/"+providerId+"/"+id+" (OAuth2Callback)");
    	super.oauth2Callback(providerId, request);
    	//super.connectionStatus(request, model);
    	ShortURL url = shortURLRepository.findByHash(id);
    	model.addAttribute("targetURL", url.getTarget());
		model.addAttribute("countDown", MainController.DEFAULT_COUNTDOWN);
		model.addAttribute("advertisement", MainController.DEFAULT_AD_PATH);
    	if (url.getAds()){
    		switch(providerId){
	    		case "facebook":
	    			if (facebook.isAuthorized()) model.addAttribute("enableAds", !ShortURLUtils.isUserInList(url, facebook));
	    			else model.addAttribute("enableAds", true);
	    			break;
	    		case "twitter":
	    			if (twitter.isAuthorized()) model.addAttribute("enableAds", !ShortURLUtils.isUserInList(url, twitter));
	    			else model.addAttribute("enableAds", true);
	    			break;
    			default:
    				break;
    		}
    	}
    	else model.addAttribute("enableAds", false);
    	return connectedView(providerId, id);
    }
    
    @RequestMapping(value = "/{providerId:facebook|twitter}/{id:(?!link|!stadistics|index).*}", method = RequestMethod.GET, params="error")
    protected String OAuth2Err(@PathVariable("providerId") String providerId, @PathVariable("id") String id, NativeWebRequest request, Model model){
    	logger.info("GET TO /connect/"+providerId+"/"+id+" (OAuth2Err)");
    	return "redirect:/connect/error";
    }

    protected String connectedView(String providerId, String url) {
    	logger.info("Redirecting to " + providerId + " view: " + url);
        return "redirect:/" + url;
    }
    
    @Override
    protected String connectedView(String providerId) {
    	logger.info("Redirecting to " + providerId + " view");
    	// User will be redirected to home, no matter the provider
        return "redirect:/";
    }

}
