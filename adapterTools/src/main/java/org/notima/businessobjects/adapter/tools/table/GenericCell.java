package org.notima.businessobjects.adapter.tools.table;

public class GenericCell {
    private Object data;
    private int color = -1;

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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String toString(){
        return data == null ? "" : getData().toString();
    }
}