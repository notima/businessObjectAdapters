package org.notima.businessobjects.adapter.tools.table;

public class GenericColumn {
    public static final String ALIGNMENT_LEFT = "left";
    public static final String ALIGNMENT_RIGHT = "right";
    public static final String ALIGNMENT_CENTER = "center";

    private String header;
    private String alignment;

    public GenericColumn() {}
    
    public GenericColumn(String header){
        this(header, ALIGNMENT_LEFT);
    }

    public GenericColumn(String header, String alignment){
        this.header = header;
        this.alignment = alignment;
    }

    public String getHeader() {
        return header;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }
    
    public void setHeader(String header) {
        this.header = header;
    }
}
