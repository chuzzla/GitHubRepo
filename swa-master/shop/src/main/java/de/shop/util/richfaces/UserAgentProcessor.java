package de.shop.util.richfaces;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import de.shop.util.mobileesp.UAgentInfo;

@Named("userAgent")
@SessionScoped
public class UserAgentProcessor implements Serializable {
	private static final long serialVersionUID = -1469351849761688348L;

	private transient UAgentInfo uAgentInfo;
    
	@Inject
	private transient HttpServletRequest request;

    @PostConstruct
    public void init() {
        final String userAgentStr = request.getHeader("user-agent");
        final String httpAccept = request.getHeader("accept");
        uAgentInfo = new UAgentInfo(userAgentStr, httpAccept);
    }

    public boolean isPhone() {
        // Detects a whole tier of phones that support similar functionality as the iphone
        return uAgentInfo.detectTierIphone();
    }

    public boolean isTablet() {
        // Will detect iPads, Xooms, Blackberry tablets, but not Galaxy - they use a strange user-agent
        return uAgentInfo.detectTierTablet();
    }

    public boolean isMobile() {
        return isPhone() || isTablet();
    }
}
