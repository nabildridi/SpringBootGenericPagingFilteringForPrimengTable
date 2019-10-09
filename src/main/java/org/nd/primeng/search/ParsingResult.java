package org.nd.primeng.search;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Sort;


public class ParsingResult {
	
	private Integer startIndex;
	private Integer pageLength;	
	
	public Sort sort;
	

	private Map<String, String> columnsFilters = new HashMap<String, String>();
	private String generalFilter;
		

	public Integer getStartIndex() {
		return startIndex;
	}
	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}
	public Integer getPageLength() {
		return pageLength;
	}
	public void setPageLength(Integer pageLength) {
		this.pageLength = pageLength;
	}
	public String getGeneralFilter() {
		return generalFilter;
	}
	public void setGeneralFilter(String generalFilter) {
		this.generalFilter = generalFilter;
	}

	public Map<String, String> getColumnsFilters() {
		return columnsFilters;
	}
	public void setColumnsFilters(Map<String, String> columnsFilters) {
		this.columnsFilters = columnsFilters;
	}
	public Sort getSort() {
		return sort;
	}
	public void setSort(Sort sort) {
		this.sort = sort;
	}
	
	
	public boolean isGeneralFiltering() {
		if(generalFilter != null && !generalFilter.isEmpty()) {
			return true;
		}else {
			return false;
		}
	}
	public boolean isColumnsFiltering() {
		if( columnsFilters.size() > 0 ) {
			return true;
		}else {
			return false;
		}
	}	

	public int getPage() {
			return startIndex/pageLength;		
	}


	
}
