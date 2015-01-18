package com.fsm.storybook.model;

public class Epub3TocItem
{
	private String title;
	private String href;
	
	public Epub3TocItem(String title, String href)
	{
		this.title = title;
		this.href = href;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getTitle()
	{
		return this.title;
	}
	public void setHref(String href)
	{
		this.href = href;
	}
	public String getHref()
	{
		return this.href;
	}
	
}