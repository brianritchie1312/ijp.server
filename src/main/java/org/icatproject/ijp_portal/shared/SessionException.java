package org.icatproject.ijp_portal.shared;

@SuppressWarnings("serial")
public class SessionException extends Exception {

	// Needed by GWT serialization
	@SuppressWarnings("unused")
	private SessionException() {
		super();
	}

	public SessionException(String msg) {
		super(msg);
	}

}
