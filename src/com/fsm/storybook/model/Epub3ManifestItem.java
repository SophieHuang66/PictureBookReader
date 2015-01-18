package com.fsm.storybook.model;

/**
 * 
 * @author shih-shinhuang
 *
 */
public class Epub3ManifestItem
{
	private String id;
	private String properties;
	private String href;
	private String mediaType;
	
	public Epub3ManifestItem(String id, String properties, String href, String mediaType)
	{
		this.id = id;
		this.properties = properties;
		this.href = href;
		this.mediaType = mediaType;
	}

	public String getId() {
		return id;
	}

	public String getHref() {
		return href;
	}

	public String getMediaType() {
		return mediaType;
	}

	public String getProperties() {
		return properties;
	}

}