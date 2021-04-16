package org.nd.primeng.search;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


public class PrimengQueries {
	
	private Pageable pageQuery;
	private String rsqlQuery = null;
	private String sortQuery = null;
	private Specification<?> spec;
	
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
	public Specification<?> getSpec() {
		return spec;
	}
	public void setSpec(Specification<?> spec) {
		this.spec = spec;
	}
	
		
}
