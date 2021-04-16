package org.nd.primeng.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ParsingResult {
	
	private Integer startIndex;
	private Integer pageLength;	

	private Map<String, List<ColumnFilter>> columnsFilters = new HashMap<String, List<ColumnFilter>>();
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

	public Map<String, List<ColumnFilter>> getColumnsFilters() {
		return columnsFilters;
	}
	public void setColumnsFilters(Map<String, List<ColumnFilter>> columnsFilters) {
		this.columnsFilters = columnsFilters;
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
