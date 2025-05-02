package org.notima.businessobjects.adapter.tools.table;

public class GenericCell {
    private Object data;
    private Object originalData;
    private int color = -1;

    private String ansiColor;
    
    public GenericCell(Object data){
        this.data = data;
    }
    
    public GenericCell(Object data, int color){
        this.data = data;
        this.color = color;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * If you format the data but want to keep a reference to the original data, use this
     * @param od
     */
    public void setOriginalData(Object od) {
    	originalData = od;
    }

    public Object getOriginalData() {
    	return originalData;
    }
    
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String toString(){
        return data == null ? "" : getData().toString();
    }
    
    public boolean hasAnsiColor() {
    	return ansiColor!=null && ansiColor.trim().length()>0;
    }
    
	public String getAnsiColor() {
		return ansiColor;
	}

	public void setAnsiColor(String ansiColor) {
		this.ansiColor = ansiColor;
	}
    
    
}