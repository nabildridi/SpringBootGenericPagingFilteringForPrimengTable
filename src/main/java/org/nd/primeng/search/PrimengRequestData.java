package org.nd.primeng.search;

import org.springframework.data.domain.Pageable;


public class PrimengRequestData {
	
	private Pageable pageSettings;
	private String rsqlQuery = null;
	
	
	public Pageable getPageSettings() {
		return pageSettings;
	}
	public void setPageSettings(Pageable pageSettings) {
		this.pageSettings = pageSettings;
	}
	public String getRsqlQuery() {
		return rsqlQuery;
	}
	public void setRsqlQuery(String rsqlQuery) {
		this.rsqlQuery = rsqlQuery;
	}
	

	
}
