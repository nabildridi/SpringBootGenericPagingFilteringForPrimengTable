package org.nd.primeng.search;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SearchBuilder {
	
	private static Logger logger = LoggerFactory.getLogger(SearchBuilder.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public PrimengRequestData process(String jsonString, Class<?> entityClass, String... fieldsOfGlobalFilter) {
		
		PrimengRequestData requestData = new PrimengRequestData();
 		
		ParsingResult parsingResult = this.parse(jsonString);
		Pageable pageSettings = this.buildPageable(parsingResult);
		requestData.setPageSettings(pageSettings);
		
		String rsqlQuery = null;
		if (parsingResult.isGeneralFiltering()) {
			rsqlQuery = this.buildGlobalFilterQuery(parsingResult, fieldsOfGlobalFilter);
		}else if (parsingResult.isColumnsFiltering()) {
			rsqlQuery = this.buildFiltersQuery(parsingResult, entityClass);
		}
		requestData.setRsqlQuery(rsqlQuery);
		
		return requestData;
	}


	public ParsingResult parse(String jsonString) {

		ParsingResult parsingResult = new ParsingResult();		

		try {

			JsonNode json = mapper.readTree(jsonString);
			
			//start index
			int first = json.get("first").asInt();
			parsingResult.setStartIndex(first);
			
			//rows per page langth
			int rows = json.get("rows").asInt();
			parsingResult.setPageLength(rows);
			
			//sorting field
			String sortingColumnName = null;
			JsonNode node = json.get("sortField");			
			if (node != null) {				
				sortingColumnName = node.textValue();
			}
			
			//sorting order
			int sortOrder = json.get("sortOrder").asInt();
			
			//build sort
			if (sortOrder == 1 && sortingColumnName != null)
				parsingResult.setSort(Sort.by(Direction.ASC, sortingColumnName));
			if (sortOrder == -1 && sortingColumnName != null)
				parsingResult.setSort(Sort.by(Direction.DESC, sortingColumnName));
			
			
			//global filter
			node = json.get("globalFilter");
			if (node != null) {
				String globalFilter = node.textValue();
				parsingResult.setGeneralFilter(globalFilter);
			}
			
			//columns filters
			JsonNode filters = json.get("filters");
			Iterator<Map.Entry<String, JsonNode>> iter = filters.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				String columnName = entry.getKey();
				if (!columnName.equals("global")) {
					String valueToSearch = entry.getValue().get("value").asText();
					parsingResult.getColumnsFilters().put(columnName, valueToSearch);
				}
			}

		} catch (Exception e) {
			return null;
		}

		return parsingResult;
	}

	public Pageable buildPageable(ParsingResult parsingResult) {

		Pageable pageSettings = null;

		if (parsingResult.getSort() != null) {
			pageSettings = PageRequest.of(parsingResult.getPage(), parsingResult.getPageLength(),
					parsingResult.getSort());
		}else {
			pageSettings = PageRequest.of(parsingResult.getPage(), parsingResult.getPageLength());
		}

		return pageSettings;

	}
	
	public String buildFiltersQuery(ParsingResult parsingResult, Class<?> entityClass) {

		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : parsingResult.getColumnsFilters().keySet()) {
			String valueToSearch = parsingResult.getColumnsFilters().get(fieldName);
			
			Class<?> fieldType = ReflectionUtils.findField(entityClass, fieldName).getType();
			
			if(fieldType.equals(LocalDateTime.class)) {

				try {
					
					LocalDateTime  start = null;
					LocalDateTime  end = null;
					
					if(valueToSearch.contains("-")) {
						//this is a date range search
						String[] rangeDates = valueToSearch.split("-");
						
						long timeStamp = Long.parseLong(rangeDates[0]);
						LocalDateTime  dateTime = LocalDateTime.ofInstant(
								Instant.ofEpochMilli(timeStamp), 
								TimeZone.getDefault().toZoneId()
				        );
						start = dateTime.with(LocalTime.of(0, 0, 0, 0));
						
						timeStamp = Long.parseLong(rangeDates[1]);
						dateTime = LocalDateTime.ofInstant(
								Instant.ofEpochMilli(timeStamp), 
								TimeZone.getDefault().toZoneId()
				        );
						end = dateTime.with(LocalTime.of(23, 59, 59, 999));
					}else {
						
						//this a one day search						
						LocalDateTime  dateTime = LocalDateTime.ofInstant(
								Instant.ofEpochMilli(Long.parseLong(valueToSearch) ), 
								TimeZone.getDefault().toZoneId()
				        );  
				
						start = dateTime.with(LocalTime.of(0, 0, 0, 0));
						end = dateTime.with(LocalTime.of(23, 59, 59, 999));
					}
					


					String query = fieldName.concat(">=").concat(start.toString());
					queries.add(query);
					
					query = fieldName.concat("<=").concat(end.toString());
					queries.add(query);
					
				} catch (Exception e) {e.printStackTrace();}
			}
			else if(fieldType.equals(String.class)) {
				String query = fieldName.concat("==^*").concat(valueToSearch).concat("*");
				queries.add(query);
			}
			else {
				String query = fieldName.concat("==").concat(valueToSearch);
				queries.add(query);
			}
			

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " and ");
		}

		return rsqlQuery;

	}

	public String buildGlobalFilterQuery(ParsingResult parsingResult, String... fieldsOfGlobalFilter) {
		
		String valueToSearch = parsingResult.getGeneralFilter();
		
		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : fieldsOfGlobalFilter) {
			String query = fieldName.concat("==^*").concat(valueToSearch).concat("*");
			queries.add(query);

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " or ");
		}

		return rsqlQuery;

	}
}
