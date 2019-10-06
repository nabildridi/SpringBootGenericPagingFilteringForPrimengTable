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
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SearchBuilder {
	
	private static Logger logger = LoggerFactory.getLogger(SearchBuilder.class);


	public PrimengRequestData parse(String jsonString, Class<?> entityClass) {

		PrimengRequestData requestData = new PrimengRequestData();
		requestData.setEntityClass(entityClass);
		
		ObjectMapper mapper = new ObjectMapper();

		try {

			JsonNode json = mapper.readTree(jsonString);
			int first = json.get("first").asInt();
			requestData.setStartIndex(first);

			int rows = json.get("rows").asInt();
			requestData.setPageLength(rows);

			JsonNode node = json.get("sortField");
			if (node != null) {
				String sortField = node.textValue();
				if (sortField != null) {
					requestData.setSortingColumnName(sortField);
				}

			}

			int sortOrder = json.get("sortOrder").asInt();
			if (sortOrder == 1)
				requestData.setSortingDirection(Direction.ASC);
			if (sortOrder == -1)
				requestData.setSortingDirection(Direction.DESC);

			node = json.get("globalFilter");
			if (node != null) {
				String globalFilter = node.textValue();
				requestData.setGeneralFilter(globalFilter);
			}

			JsonNode filters = json.get("filters");
			Iterator<Map.Entry<String, JsonNode>> iter = filters.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				String columnName = entry.getKey();
				if (!columnName.equals("global")) {
					String valueToSearch = entry.getValue().get("value").asText();
					requestData.getColumnsFilters().put(columnName, valueToSearch);
				}
			}

		} catch (Exception e) {
			return null;
		}

		return requestData;
	}

	public Pageable buildPageable(PrimengRequestData requestData) {

		Pageable pageSettings = PageRequest.of(requestData.getPage(), requestData.getPageLength());

		if (requestData.getSpringSort() != null) {
			pageSettings = PageRequest.of(requestData.getPage(), requestData.getPageLength(),
					requestData.getSpringSort());
		}

		return pageSettings;

	}
	
	public String buildFiltersQuery(PrimengRequestData requestData) {

		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();
		
		Class<?> entityClass = requestData.getEntityClass();

		for (String fieldName : requestData.getColumnsFilters().keySet()) {
			String valueToSearch = requestData.getColumnsFilters().get(fieldName);
			
			Class<?> fieldType = ReflectionUtils.findField(entityClass, fieldName).getType();
			
			if(fieldType != null && fieldType.equals(LocalDateTime.class)) {

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

	public String buildGlobalFilterQuery(PrimengRequestData requestData, String... fieldsToApply) {
		
		String valueToSearch = requestData.getGeneralFilter();
		
		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : fieldsToApply) {
			String query = fieldName.concat("==^*").concat(valueToSearch).concat("*");
			queries.add(query);

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " or ");
		}

		return rsqlQuery;

	}
}
