package com.fsm.storybook.model;

import java.util.Date;

public class AuthStatus {

	private String userId = null;
	private String userPassword;
	private String unitId;
	private Date loginDateTime;
	
	public AuthStatus() {
		
	}
	
	public AuthStatus (String userId, String userPassword, String unitId) {
		this.userId = userId;
		this.userPassword = userPassword;
		this.unitId = unitId;
		this.loginDateTime = new Date(System.currentTimeMillis());
	}
	
	public boolean isLogined() {
		if (userId!=null) 
			return true;
		else 
			return false;
	}
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserPassword() {
		return userPassword;
	}
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public Date getLoginDateTime() {
		return loginDateTime;
	}
	public void setLoginDateTime(Date loginDateTime) {
		this.loginDateTime = loginDateTime;
	}
	
}
