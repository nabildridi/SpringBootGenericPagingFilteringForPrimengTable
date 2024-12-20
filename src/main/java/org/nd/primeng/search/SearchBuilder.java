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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.github.perplexhub.rsql.RSQLJPASupport;

@Service
public class SearchBuilder {

	private static Logger logger = LoggerFactory.getLogger(SearchBuilder.class);

	private ObjectMapper mapper = new ObjectMapper();

	private Pattern numericPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

	public PrimengQueries process(String primengRequestJson, Class<?> entityClass, String... fieldsOfGlobalFilter) {

		PrimengQueries requestData = new PrimengQueries();

		// to json object
		JsonNode primengRequestNode = toJsonNode(primengRequestJson);

		ParsingResult parsingResult = this.parse(primengRequestNode, entityClass);

		// build pagination data
		Pageable pageQuery = this.buildPageable(parsingResult);
		requestData.setPageQuery(pageQuery);

		// build sort
		String sortQuery = buildSortQuery(primengRequestNode);
		requestData.setSortQuery(sortQuery);

		// build filtering query
		String rsqlQuery = null;
		if (parsingResult.isGeneralFiltering() && fieldsOfGlobalFilter != null) {
			rsqlQuery = this.buildGlobalFilterQuery(parsingResult, entityClass, fieldsOfGlobalFilter);
		} else if (parsingResult.isColumnsFiltering()) {
			rsqlQuery = this.buildFiltersQuery(parsingResult);
		}

		Specification<?> spec = getSpec(entityClass, rsqlQuery, sortQuery);
		requestData.setSpec(spec);

		if (rsqlQuery == null)
			rsqlQuery = "";
		requestData.setRsqlQuery(rsqlQuery);

		return requestData;
	}

	private <T> Specification<T> getSpec(Class<T> entityClass, String rsqlQuery, String sortQuery) {

		Specification<T> spec = null;
		if (rsqlQuery == null) {
			spec = RSQLJPASupport.toSort(sortQuery);
		} else {
			spec = RSQLJPASupport.<T>toSpecification(rsqlQuery).and(RSQLJPASupport.toSort(sortQuery));
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

	private ParsingResult parse(JsonNode primengRequestNode, Class<?> entityClass) {

		ParsingResult parsingResult = new ParsingResult();

		try {

			// start index
			int first = primengRequestNode.get("first").asInt();
			parsingResult.setStartIndex(first);

			// rows per page langth
			int rows = primengRequestNode.get("rows").asInt();
			parsingResult.setPageLength(rows);

			// global filter
			JsonNode node = primengRequestNode.get("globalFilter");
			if (node != null) {
				String globalFilter = node.textValue();
				parsingResult.setGeneralFilter(globalFilter);
			}

			// columns filters
			JsonNode filters = primengRequestNode.get("filters");
			Iterator<Map.Entry<String, JsonNode>> iter = filters.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				String columnName = entry.getKey();
				JsonNode filterNode = entry.getValue();

				List<ColumnFilter> filtersOfField = new ArrayList<ColumnFilter>();

				if (!columnName.equals("global")) {

					if (filterNode.isArray()) {

						Iterator<JsonNode> rulesIter = filterNode.iterator();
						while (rulesIter.hasNext()) {
							JsonNode ruleEntry = rulesIter.next();

							if (isValueNotNullAndNotEmpty(ruleEntry)) {
								ColumnFilter columnFilter = extractFilterData(ruleEntry, columnName, entityClass);
								filtersOfField.add(columnFilter);
							}
						}

					}

					if (filterNode.isObject()) {

						if (isValueNotNullAndNotEmpty(filterNode)) {
							ColumnFilter columnFilter = extractFilterData(filterNode, columnName, entityClass);
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
			// this is multisort
			for (JsonNode multiSortItem : multiSortMetaArray) {
				String sortField = multiSortItem.get("field").textValue();

				int sortOrderInt = multiSortItem.get("order").asInt();
				String sortOrder = null;
				if (sortOrderInt == 1)
					sortOrder = "asc";
				if (sortOrderInt == -1)
					sortOrder = "desc";

				queries.add(sortField + "," + sortOrder);

			}
		} else {
			// Single sort

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

			if (sortOrderInt != null && sortField != null) {
				String sortOrder = null;
				if (sortOrderInt.intValue() == 1)
					sortOrder = "asc";
				if (sortOrderInt.intValue() == -1)
					sortOrder = "desc";

				queries.add(sortField + "," + sortOrder);
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

			if (ColumnFiltersList.size() == 1) {
				ColumnFilter cf = ColumnFiltersList.get(0);
				String query = getColumnQuery(cf, fieldName);
				queries.add(query);
			}

			if (ColumnFiltersList.size() > 1) {
				String localOperator = ColumnFiltersList.get(0).getOperator();
				List<String> groupedQueries = new ArrayList<String>();

				for (ColumnFilter cf : ColumnFiltersList) {

					String query = getColumnQuery(cf, fieldName);
					groupedQueries.add(query);
				}

				String localQuery = "(" + StringUtils.collectionToDelimitedString(groupedQueries, " " + localOperator + " ") + ")";
				queries.add(localQuery);

			}

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " and ");
		}

		return rsqlQuery;
	}

	private String buildGlobalFilterQuery(ParsingResult parsingResult, Class<?> entityClass, String... fieldsOfGlobalFilter) {

		String valueToSearch = parsingResult.getGeneralFilter();

		valueToSearch = escapeSpecialChars(valueToSearch);

		String rsqlQuery = null;
		List<String> queries = new ArrayList<String>();

		for (String fieldName : fieldsOfGlobalFilter) {

			ColumnType columnType = findType(fieldName, entityClass);
			if (columnType == ColumnType.NUMERIC && !isNumeric(valueToSearch)) {
				continue;
			}
			String rsqlFragment = getRsqlFragmentForMatchMode("default", columnType);
			rsqlFragment = rsqlFragment.replace("[placeholder]", valueToSearch);
			String query = fieldName + rsqlFragment;
			queries.add(query);

		}

		if (queries.size() > 0) {
			rsqlQuery = StringUtils.collectionToDelimitedString(queries, " or ");
		}

		return rsqlQuery;

	}

	private String escapeSpecialChars(String valueToSearch) {

		valueToSearch = valueToSearch.replace("\\", "\\\\\\\\");

		String[] specialChars = { "'" };
		for (String special : specialChars) {
			valueToSearch = valueToSearch.replace(special, "\\".concat(special));
		}

		return valueToSearch;

	}

	private String getDatesQuery(ColumnFilter cf, String rsqlFragment, String fieldName) {

		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.parse(cf.getValueToSearch()), TimeZone.getDefault().toZoneId());

		LocalDateTime start = dateTime.with(LocalTime.of(0, 0, 0, 0));
		LocalDateTime end = dateTime.with(LocalTime.of(23, 59, 59, 999));

		rsqlFragment = rsqlFragment.replace("[startDay]", start.toString());
		rsqlFragment = rsqlFragment.replace("[endDay]", end.toString());
		rsqlFragment = fieldName.concat(rsqlFragment);
		String query = "(".concat(rsqlFragment).concat(")");

		return query;

	}

	private String getRsqlFragmentForMatchMode(String matchMode, ColumnType type) {

		String operator = null;

		if (type == ColumnType.TEXT) {
			if (matchMode.equals("contains") || matchMode.equals("default"))
				operator = "=ilike=\"[placeholder]\"";
			if (matchMode.equals("startsWith"))
				operator = "==\"^[placeholder]*\"";
			if (matchMode.equals("notContains"))
				operator = "=inotlike=\"[placeholder]\"";
			if (matchMode.equals("endsWith"))
				operator = "==\"^*[placeholder]\"";
			if (matchMode.equals("equals"))
				operator = "==\"[placeholder]\"";
			if (matchMode.equals("notEquals"))
				operator = "!=\"[placeholder]\"";
			if (matchMode.equals("in"))
				operator = "=in=[placeholder]";
		}

		if (type == ColumnType.NUMERIC) {
			if (matchMode.equals("equals") || matchMode.equals("default"))
				operator = "==[placeholder]";
			if (matchMode.equals("notEquals"))
				operator = "!=[placeholder]";
			if (matchMode.equals("lt"))
				operator = "<[placeholder]";
			if (matchMode.equals("lte"))
				operator = "<=[placeholder]";
			if (matchMode.equals("gt"))
				operator = ">[placeholder]";
			if (matchMode.equals("gte"))
				operator = ">=[placeholder]";
			if (matchMode.equals("in"))
				operator = "=in=[placeholder]";
		}

		if (type == ColumnType.BOOLEAN) {
			if (matchMode.equals("equals") || matchMode.equals("default"))
				operator = "==[placeholder]";
		}

		if (type == ColumnType.DATE) {
			if (matchMode.equals("dateIs") || matchMode.equals("default"))
				operator = "=bt=('[startDay]', '[endDay]')";
			if (matchMode.equals("dateIsNot"))
				operator = "=nb=('[startDay]', '[endDay]')";
			;
			if (matchMode.equals("dateBefore"))
				operator = "<'[startDay]'";
			if (matchMode.equals("dateAfter"))
				operator = ">'[endDay]'";
		}

		return operator;

	}

	private ColumnFilter extractFilterData(JsonNode jsonFilter, String fieldName, Class<?> entityClass) {

		ColumnFilter columnFilter = new ColumnFilter();

		JsonNode valueNode = jsonFilter.get("value");

		String valueToSearch = null;
		if (valueNode.isArray()) {
			String value = valueNode.toString();

			valueToSearch = value.replace("[", "(").replace("]", ")");
			if (valueToSearch.equals("()"))
				valueToSearch = null;
			columnFilter.setValueToSearch(valueToSearch);
			columnFilter.setMatchMode("in");

		} else {
			valueToSearch = jsonFilter.get("value").asText();

			valueToSearch = escapeSpecialChars(valueToSearch);
			columnFilter.setValueToSearch(valueToSearch);

			JsonNode matchModeNode = jsonFilter.get("matchMode");
			if (matchModeNode != null) {
				columnFilter.setMatchMode(matchModeNode.asText());
			}

		}

		JsonNode operatorNode = jsonFilter.get("operator");
		if (operatorNode != null) {
			columnFilter.setOperator(operatorNode.asText());
		}

		ColumnType type = findType(fieldName, entityClass);
		columnFilter.setType(type);

		return columnFilter;

	}

	private ColumnType findType(String fieldName, Class<?> entityClass) {

		Class<?> fieldType = ReflectionUtils.findField(entityClass, fieldName).getType();

		if (fieldType.equals(LocalDateTime.class) || fieldType.equals(Date.class)) {
			return ColumnType.DATE;
		} else if (fieldType.equals(String.class)) {
			return ColumnType.TEXT;
		} else if (fieldType.equals(Boolean.class)) {
			return ColumnType.BOOLEAN;
		} else {
			return ColumnType.NUMERIC;
		}

	}

	private String getColumnQuery(ColumnFilter cf, String fieldName) {

		String query = null;
		String rsqlFragment = getRsqlFragmentForMatchMode(cf.getMatchMode(), cf.getType());

		if (cf.getType() != ColumnType.DATE) {
			rsqlFragment = rsqlFragment.replace("[placeholder]", cf.getValueToSearch());
			query = fieldName + rsqlFragment;
		} else {
			query = getDatesQuery(cf, rsqlFragment, fieldName);

		}

		return query;
	}

	private boolean isValueNotNullAndNotEmpty(JsonNode nodeToTest) {

		boolean result = true;

		if (nodeToTest.get("value").isArray()) {
			ArrayNode array = (ArrayNode) nodeToTest.get("value");
			if (array.isEmpty())
				result = false;
		} else {
			if (nodeToTest.get("value").isNull())
				result = false;

		}

		return result;
	}

	public boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		return numericPattern.matcher(strNum).matches();
	}

}
