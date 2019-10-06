package org.nd.primeng.search;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;


public class PrimengRequestData {
	
	private Class<?> entityClass;
	
	private Integer startIndex;
	private Integer pageLength;	
	
	private Direction sortingDirection;
	private String sortingColumnName;

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
	
	public Direction getSortingDirection() {
		return sortingDirection;
	}
	public void setSortingDirection(Direction sortingDirection) {
		this.sortingDirection = sortingDirection;
	}
	public String getSortingColumnName() {
		return sortingColumnName;
	}
	public void setSortingColumnName(String sortingColumnName) {
		this.sortingColumnName = sortingColumnName;
	}
	
	public Sort getSpringSort() {
		if(sortingColumnName != null) {
			return Sort.by(sortingDirection, sortingColumnName);
		}else {
			return null;
		}
		
	}	

	public int getPage() {
			return startIndex/pageLength;		
	}
	
	public Class<?> getEntityClass() {
		return entityClass;
	}
	
	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}
	
	
}
