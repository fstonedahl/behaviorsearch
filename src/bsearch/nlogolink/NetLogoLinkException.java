package bsearch.nlogolink;

import bsearch.app.BehaviorSearchException;

//TODO: Get rid of NetLogoLinkException, and just use BehaviorSearchException for everything?  Consider...
public class NetLogoLinkException extends BehaviorSearchException {

	private static final long serialVersionUID = 1L;

	public NetLogoLinkException(String message) {
		super(message);
	}
	
	public NetLogoLinkException(String message, Throwable cause) {
		super(message, cause);
	}

}
