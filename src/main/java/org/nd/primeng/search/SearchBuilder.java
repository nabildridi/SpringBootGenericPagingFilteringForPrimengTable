package org.nd.primeng.search;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.perplexhub.rsql.RSQLJPASupport;

@Service
public class SearchBuilder {
	
	private static Logger logger = LoggerFactory.getLogger(SearchBuilder.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public PrimengQueries process(String primengRequestJson, Class<?> entityClass, String... fieldsOfGlobalFilter) {
		
		PrimengQueries requestData = new PrimengQueries();
		
		//to json object
		JsonNode primengRequestNode = toJsonNode(primengRequestJson);
 		
		ParsingResult parsingResult = this.parse(primengRequestNode);
		
		//build pagination data
		Pageable pageQuery = this.buildPageable(parsingResult);
		requestData.setPageQuery(pageQuery);
		
		//build sort
		String sortQuery =  buildSortQuery(primengRequestNode);
		requestData.setSortQuery(sortQuery);
		
		//build filtering query
		String rsqlQuery = null;
		if (parsingResult.isGeneralFiltering() && fieldsOfGlobalFilter != null) {
			rsqlQuery = this.buildGlobalFilterQuery(parsingResult, fieldsOfGlobalFilter);
		}else if (parsingResult.isColumnsFiltering()) {
			rsqlQuery = this.buildFiltersQuery(parsingResult);
		}
	
		if(rsqlQuery==null)rsqlQuery="";
		requestData.setRsqlQuery(rsqlQuery);	
		
		Specification<?> spec = getSpec(entityClass, rsqlQuery, sortQuery);
		requestData.setSpec(spec);
		
		
		return requestData;
	}
	
	private <T> Specification<T> getSpec(Class<T> clazz, String rsqlQuery, String sortQuery) {
		   
			Specification<T> spec = null;
		   if (rsqlQuery == null) {
				spec = RSQLJPASupport.toSort(sortQuery);
			} else {
				spec = (Specification<T>) RSQLJPASupport.toSpecification(rsqlQuery).and(RSQLJPASupport.toSort(sortQuery));
			}
		   
		   return spec;
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
				JsonNode filterNode = entry.getValue();
				
				List<ColumnFilter> filtersOfField = new ArrayList<ColumnFilter>();
				
				
				
				if (!columnName.equals("global")) {	
					
					
					
					
					if(filterNode.isArray()) {
						
						Iterator<JsonNode> rulesIter = filterNode.iterator();
						while (rulesIter.hasNext()) {
							JsonNode ruleEntry = rulesIter.next();
							
							if(!ruleEntry.get("value").isNull()) {
								ColumnFilter columnFilter = extractFilterData(ruleEntry);
								filtersOfField.add(columnFilter);
							}
						}
						
					}
					
					if(filterNode.isObject()) {						
						
						if(!filterNode.get("value").isNull()) {
							ColumnFilter columnFilter = extractFilterData(filterNode);
							filtersOfField.add(columnFilter);
						}				
						
					}
					
					parsingResult.getColumnsFilters().put(columnName, filtersOfField);
					
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

		return sortQuery;
	}
	
	
	
	private Pageable buildPageable(ParsingResult parsingResult) {

		Pageable pageQuery = PageRequest.of(parsingResult.getPage(), parsingResult.getPageLength());
		return pageQuery;

	}
	
	private String buildFiltersQuery(ParsingResult parsingResult) {

		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : parsingResult.getColumnsFilters().keySet()) {
			
			List<ColumnFilter> ColumnFiltersList = parsingResult.getColumnsFilters().get(fieldName);
			
			if(ColumnFiltersList.size() == 1) {
				ColumnFilter cf = ColumnFiltersList.get(0);
				String rsqlFragment = getRsqlFragmentForMatchMode(cf.getMatchMode(), cf.getType());
				
				if(cf.getType() != ColumnType.DATE) {
					rsqlFragment = rsqlFragment.replace("[placeholder]", cf.getValueToSearch());
					String query = fieldName + rsqlFragment; 
					queries.add(query);
				}else {
					String query = getDatesQuery(cf, rsqlFragment, fieldName); 
					queries.add(query);
				}
				
			}
			
			if(ColumnFiltersList.size() > 1) {
				String localOperator = ColumnFiltersList.get(0).getOperator();
				List<String> groupedQueries = new ArrayList<String>();
				
				for(ColumnFilter cf : ColumnFiltersList) {
					String rsqlFragment = getRsqlFragmentForMatchMode(cf.getMatchMode(), cf.getType());

					if(cf.getType() != ColumnType.DATE) {
						rsqlFragment = rsqlFragment.replace("[placeholder]", cf.getValueToSearch());
						String query = fieldName + rsqlFragment; 
						groupedQueries.add(query);
					}else {
						String query = getDatesQuery(cf, rsqlFragment, fieldName); 
						groupedQueries.add(query);
					}
				}
				
				String localQuery  = "(" + StringUtils.collectionToDelimitedString(groupedQueries, " " + localOperator+ " ") + ")";
				queries.add(localQuery);

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
		
		valueToSearch = valueToSearch.replace("\\", "\\\\\\\\");
		
		String[] specialChars = {"'"};
		for(String special : specialChars) {
			valueToSearch = valueToSearch.replace(special, "\\".concat(special));
		}		
		
		return valueToSearch;
		
	}
	
	private ColumnType findType(JsonNode valueNode) {
		
		if(valueNode.isNumber())return ColumnType.NUMERIC;
		if(valueNode.isTextual()) {
			String value = valueNode.textValue();
			
			try {
				Date.from( Instant.parse( value ));
				
				return ColumnType.DATE;
			} catch (Exception e) {
				return ColumnType.TEXT;
			}
			
		}	
		
	    return ColumnType.TEXT;    
	}
	
	
	private String getDatesQuery(ColumnFilter cf, String rsqlFragment, String fieldName) {
		
		LocalDateTime  dateTime = LocalDateTime.ofInstant(
				Instant.parse( cf.getValueToSearch() ), 
				TimeZone.getDefault().toZoneId()
        );  

		LocalDateTime start = dateTime.with(LocalTime.of(0, 0, 0, 0));
		LocalDateTime end = dateTime.with(LocalTime.of(23, 59, 59, 999)); 
		
		rsqlFragment = rsqlFragment.replace("[field]", fieldName);
		rsqlFragment = rsqlFragment.replace("[startDay]", start.toString());
		rsqlFragment = rsqlFragment.replace("[endDay]", end.toString());				
		rsqlFragment = fieldName.concat(rsqlFragment);					
		String query = "(".concat(rsqlFragment).concat(")");
		
		return query;
		
	}
	
	private String getRsqlFragmentForMatchMode(String matchMode, ColumnType type) {
		
		String operator = null;
		
		if(type == ColumnType.TEXT) {
			if(matchMode.equals("default"))operator = "==\"^*[placeholder]*\"";
			if(matchMode.equals("startsWith"))operator = "==\"[placeholder]*\"";
		    if(matchMode.equals("contains"))operator = "==\"^*[placeholder]*\"";
		    if(matchMode.equals("notContains"))operator = "!=\"^*[placeholder]*\"";
		    if(matchMode.equals("endsWith"))operator = "==\"*[placeholder]\"";
		    if(matchMode.equals("equals"))operator = "==\"[placeholder]\"";
		    if(matchMode.equals("notEquals"))operator = "!=\"[placeholder]\"";
		}
		
		if(type == ColumnType.NUMERIC) {
			if(matchMode.equals("default"))operator = "==[placeholder]";
		    if(matchMode.equals("equals"))operator = "==[placeholder]";
		    if(matchMode.equals("notEquals"))operator = "!=[placeholder]";
		    if(matchMode.equals("lt"))operator = "<[placeholder]";
		    if(matchMode.equals("lte"))operator = "<=[placeholder]";
		    if(matchMode.equals("gt"))operator = ">[placeholder]";
		    if(matchMode.equals("gte"))operator = ">=[placeholder]";
		}
		
		if(type == ColumnType.DATE) {
			if(matchMode.equals("default"))operator = ">=[startDay] and [field]<=[endDay]";
			if(matchMode.equals("dateIs"))operator = ">=[startDay] and [field]<=[endDay]";
		    if(matchMode.equals("dateIsNot"))operator = "<[startDay] or [field]>[endDay]";
		    if(matchMode.equals("dateBefore"))operator = "<[startDay]";
		    if(matchMode.equals("dateAfter"))operator = ">[endDay]";
		}
	    
	    return operator;
	    
	}
	
	private ColumnFilter extractFilterData(JsonNode jsonFilter) {
		
		ColumnFilter columnFilter = new ColumnFilter();

		String valueToSearch = jsonFilter.get("value").asText();							
		columnFilter.setValueToSearch(valueToSearch);
		
		
		JsonNode matchModeNode = jsonFilter.get("matchMode");
		if(matchModeNode != null) {
			columnFilter.setMatchMode(matchModeNode.asText());
		}
		
		JsonNode operatorNode = jsonFilter.get("operator");
		if(operatorNode != null) {
			columnFilter.setOperator(operatorNode.asText());
		}
		
		ColumnType type = findType(jsonFilter.get("value"));
		columnFilter.setType(type);
		
		return columnFilter;

	}
		
	
}
