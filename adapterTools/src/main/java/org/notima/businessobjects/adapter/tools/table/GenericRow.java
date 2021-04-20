package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GenericRow {

    private List<Object> data;
    
    GenericRow() {
        data = new ArrayList<>();
    }
    
    GenericRow(List<GenericColumn> cols) {
        this();
        for (GenericColumn col : cols) {
            data.add(col.getHeader());
        }
    }

    public void addGenericContent(List<GenericCell> data) {
    	if (data==null) return;
    	this.data.addAll(Arrays.asList(data));
    }
    
    public void addContent(List<Object> data) {
        this.data = data;
    }

    public void addContent(Object ... cellDataAr) {
        data.addAll(Arrays.asList(cellDataAr));
    }

    public List<Object> getContent() {
    	return data;
    }
	
}
