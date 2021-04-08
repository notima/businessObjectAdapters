package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.List;

public class HtmlTable {
    List<GenericColumn> columns = new ArrayList<GenericColumn>();
    List<List<Object>> rows = new ArrayList<List<Object>>();

    public void column(String header){
        column(header, null);
    }
    
    public void column(String header, String alignment){
        GenericColumn column = new GenericColumn();
        column.setHeader(header);
        column.setAlignment(alignment);
        columns.add(column);
    }

    public void addRow(List<Object> row){
        rows.add(row);
    }

    public String toString(){
        StringBuffer htmlBuffer = new StringBuffer();
        htmlBuffer.append("<table>");
        addTableHeadersToBuffer(htmlBuffer);
        addRowsToBuffer(htmlBuffer);
        htmlBuffer.append("</table>");
        return htmlBuffer.toString();
    }

    private void addTableHeadersToBuffer(StringBuffer htmlBuffer){
        htmlBuffer.append("<tr>");

        for(GenericColumn column : columns){
            htmlBuffer.append(String.format("<th>%s</th>", column.getHeader()));
        }

        htmlBuffer.append("</tr>");
    }

    private void addRowsToBuffer(StringBuffer htmlBuffer){

        for(List<Object> row : rows){
            htmlBuffer.append("<tr>");
            for(int i = 0; i < row.size(); i++){
                GenericColumn column = columns.get(i);
                String alignement = column.getAlignment();
                htmlBuffer.append(String.format("<td %s>", alignement != null ? String.format("style=\"text-align: %s;\"", alignement) : ""));
                Object data = row.get(i);
                if(data != null)
                    htmlBuffer.append(data.toString());
                htmlBuffer.append("</td>");
            }
            htmlBuffer.append("</tr>");
        }
    }
}
