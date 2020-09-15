package org.nd.primeng.search;

import org.springframework.data.domain.Pageable;


public class PrimengQueries {
	
	private Pageable pageQuery;
	private String rsqlQuery = null;
	private String sortQuery = null;
	
	public Pageable getPageQuery() {
		return pageQuery;
	}
	public void setPageQuery(Pageable pageQuery) {
		this.pageQuery = pageQuery;
	}
	public String getRsqlQuery() {
		return rsqlQuery;
	}
	public void setRsqlQuery(String rsqlQuery) {
		this.rsqlQuery = rsqlQuery;
	}
	
	public String getSortQuery() {
		return sortQuery;
	}
	public void setSortQuery(String sortQuery) {
		this.sortQuery = sortQuery;
	}
	
	
	
}
