package io.github.infolis.model;

/**
 * Predefined media types for input streams that InfoLink supports.
 * 
 * @author kba
 */
public enum InfolisMediaType {
	TextPlain("text/plain"),
	ApplicationPdf("application/pdf");

	private final String mediaType;

	private InfolisMediaType(String asString) {
		this.mediaType = asString;
	}

	public String getMediaType() {
		return mediaType;
	}

}
