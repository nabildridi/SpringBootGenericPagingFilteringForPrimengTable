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
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SearchBuilder {
	
	private static Logger logger = LoggerFactory.getLogger(SearchBuilder.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public PrimengQueries process(String primengRequestJson, Class<?> entityClass, String... fieldsOfGlobalFilter) {
		
		PrimengQueries requestData = new PrimengQueries();
		
		JsonNode primengRequestNode = toJsonNode(primengRequestJson);
 		
		ParsingResult parsingResult = this.parse(primengRequestNode);
		Pageable pageQuery = this.buildPageable(parsingResult);
		requestData.setPageQuery(pageQuery);
		
		//build sort
		String sortQuery =  buildSortQuery(primengRequestNode);
		requestData.setSortQuery(sortQuery);
		
		//build filtering query
		String rsqlQuery = null;
		if (parsingResult.isGeneralFiltering()) {
			rsqlQuery = this.buildGlobalFilterQuery(parsingResult, fieldsOfGlobalFilter);
		}else if (parsingResult.isColumnsFiltering()) {
			rsqlQuery = this.buildFiltersQuery(parsingResult, entityClass);
		}
		
		requestData.setRsqlQuery(rsqlQuery);	
		
		return requestData;
	}
	
	private JsonNode toJsonNode(String primengRequestJson) {
		try {

			JsonNode primengRequestNode = mapper.readTree(primengRequestJson);
			return primengRequestNode;
			
		} catch (Exception e) {
			return null;
		}		
	}


	private ParsingResult parse(JsonNode primengRequestNode) {

		ParsingResult parsingResult = new ParsingResult();		

		try {
			
			//start index
			int first = primengRequestNode.get("first").asInt();
			parsingResult.setStartIndex(first);
			
			//rows per page langth
			int rows = primengRequestNode.get("rows").asInt();
			parsingResult.setPageLength(rows);
			
			
			//global filter
			JsonNode node = primengRequestNode.get("globalFilter");
			if (node != null) {
				String globalFilter = node.textValue();
				parsingResult.setGeneralFilter(globalFilter);
			}
			
			//columns filters
			JsonNode filters = primengRequestNode.get("filters");
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

	
	private String buildSortQuery(JsonNode primengRequestNode) {
		
		
		List<String> queries = new ArrayList<String>();
		
		JsonNode multiSortMetaArray = primengRequestNode.get("multiSortMeta");			
		if (multiSortMetaArray != null) {
			//this is multisort
			for(JsonNode multiSortItem : multiSortMetaArray) {
				String sortField = multiSortItem.get("field").textValue();
			
				int sortOrderInt = multiSortItem.get("order").asInt();
				String sortOrder = null;
				if(sortOrderInt == 1)sortOrder="asc";
				if(sortOrderInt == -1)sortOrder="desc";
				
				queries.add(sortField+","+sortOrder);
				
			}
		}else {
			//Single sort
			
			String sortField = null;
			JsonNode node = primengRequestNode.get("sortField");			
			if (node != null) {				
				sortField = node.textValue();
			}
			
			Integer sortOrderInt = null;
			node = primengRequestNode.get("sortOrder");
			if (node != null) {				
				sortOrderInt = node.asInt();
			}
			
			
			if(sortOrderInt != null && sortField != null) {
				String sortOrder = null;
				if(sortOrderInt.intValue() == 1)sortOrder="asc";
				if(sortOrderInt.intValue() == -1)sortOrder="desc";
				
				queries.add(sortField+","+sortOrder);
			}
			
			
			
		}
		
		String sortQuery = StringUtils.collectionToDelimitedString(queries, ";");
		
		logger.debug(sortQuery);
		return sortQuery;
	}
	
	
	
	private Pageable buildPageable(ParsingResult parsingResult) {

		Pageable pageQuery = PageRequest.of(parsingResult.getPage(), parsingResult.getPageLength());
		return pageQuery;

	}
	
	private String buildFiltersQuery(ParsingResult parsingResult, Class<?> entityClass) {

		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : parsingResult.getColumnsFilters().keySet()) {
			String valueToSearch = parsingResult.getColumnsFilters().get(fieldName);
			
			Class<?> fieldType = ReflectionUtils.findField(entityClass, fieldName).getType();
			
			valueToSearch = escapeSpecialChars(valueToSearch);
			
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
				String query = fieldName.concat("==\"^*").concat(valueToSearch).concat("*\"");
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

	private String buildGlobalFilterQuery(ParsingResult parsingResult, String... fieldsOfGlobalFilter) {
		
		String valueToSearch = parsingResult.getGeneralFilter();
		
		valueToSearch = escapeSpecialChars(valueToSearch);
		
		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : fieldsOfGlobalFilter) {
			String query = fieldName.concat("==\"^*").concat(valueToSearch).concat("*\"");
			queries.add(query);

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " or ");
		}

		return rsqlQuery;

	}
	
	private String escapeSpecialChars(String valueToSearch) {
				
		String[] specialChars = {"\"", "'"};
		for(String special : specialChars) {
			valueToSearch = valueToSearch.replace(special, "\\".concat(special));
		}
		
		valueToSearch = valueToSearch.replace("\\", "\\\\\\\\");
		
		return valueToSearch;
		
	}
}
