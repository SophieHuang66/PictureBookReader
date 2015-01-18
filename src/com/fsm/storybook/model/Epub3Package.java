package com.fsm.storybook.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Epub3Package {

	public static final String DIRECTION_RTL = "rtl";
	public static final String DIRECTION_LTR = "ltr";
	
	private String rootPath;
	private String contentPath;
	private String pageProgressionDirection;
	private Map<String, String> metaData = null;
	private List<Epub3ManifestItem> manifestItems = null;
	private List<Epub3ManifestItem> spineItems = null;
	private List<Epub3TocItem> tocItems = null;
	
	public Epub3Package(String rootPath)
	{
		this.setRootPath(rootPath);
	}
	
	public void setContentPath(String contentPath)
	{
		this.contentPath = contentPath;
	}
	
	public String getContentPath()
	{
		return this.contentPath;
	}
	
	public String getPageProgressionDirection() {
		return pageProgressionDirection;
	}


	public void setPageProgressionDirection(String pageProgressionDirection) {
		this.pageProgressionDirection = pageProgressionDirection;
	}


	public String getRootPath() {
		return rootPath;
	}


	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}


	public Map<String, String> getMetaData() {
		return metaData;
	}


	public void setMetaData(Map<String, String> metaData) {
		this.metaData = metaData;
	}


	public List<Epub3ManifestItem> getManifestItems() {
		return manifestItems;
	}


	public void setManifestItems(List<Epub3ManifestItem> manifestItems) {
		this.manifestItems = manifestItems;
	}


	public List<Epub3ManifestItem> getSpineItems() {
		return spineItems;
	}


	public void setSpineItems(List<Epub3ManifestItem> spineItems) {
		this.spineItems = spineItems;
	}


	public List<Epub3TocItem> getTocItems() {
		return tocItems;
	}


	public void setTocItems(List<Epub3TocItem> tocItems) {
		this.tocItems = tocItems;
	}

	public String getCoverImage() {
		String coverImage = null;
		if (manifestItems!=null)
		{
			for(Epub3ManifestItem item : manifestItems) {
				if (item.getProperties()!=null && item.getProperties().equals("cover-image")) {
					coverImage = item.getHref();
				}
			}
		}
		return coverImage;
	}

	public String getNavPage() {
		String navPage = null;
		if (manifestItems!=null)
		{
			for(Epub3ManifestItem item : manifestItems) {
				if (item.getProperties()!=null && item.getProperties().equals("nav")) {
					navPage = item.getHref();
				}
			}
		}
		return navPage;
	}
	
	public List<String> getStyleItems()
	{
		ArrayList<String> styleItems = new ArrayList<String>();
		if (manifestItems!=null)
		{
			for(Epub3ManifestItem item : manifestItems) {
				if (item.getProperties()!=null && item.getProperties().equals("style")) {
					styleItems.add(item.getHref());
				}
			}
		}
		return styleItems;
	}
	
}
