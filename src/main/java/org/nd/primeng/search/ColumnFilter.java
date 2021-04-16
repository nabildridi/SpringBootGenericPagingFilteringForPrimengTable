package org.nd.primeng.search;

public class ColumnFilter {
	
	private String valueToSearch;
	private String matchMode= "default";
	private String operator = null;
	private ColumnType type;
	
	public String getValueToSearch() {
		return valueToSearch;
	}
	public void setValueToSearch(String valueToSearch) {
		this.valueToSearch = valueToSearch;
	}
	public String getMatchMode() {
		return matchMode;
	}
	public void setMatchMode(String matchMode) {
		this.matchMode = matchMode;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public ColumnType getType() {
		return type;
	}
	public void setType(ColumnType type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return "ColumnFilter [valueToSearch=" + valueToSearch + ", matchMode=" + matchMode + ", operator=" + operator
				+ ", type=" + type + "]";
	}
	
	

}
