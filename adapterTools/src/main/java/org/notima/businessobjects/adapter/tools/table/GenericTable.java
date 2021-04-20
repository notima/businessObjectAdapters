package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.businessobjects.adapter.tools.table.GenericColumn;


public class GenericTable {

    public static final int COLOR_BLACK = 0;
    public static final int COLOR_RED = 1;
    public static final int COLOR_GREEN = 2;
    public static final int COLOR_YELLOW = 3;
    public static final int COLOR_BLUE = 4;
    public static final int COLOR_MAGENTA = 5;
    public static final int COLOR_CYAN = 6;
    public static final int COLOR_WHITE = 7;


    public String[] ansiColors = {
        "\u001B[30m",
        "\u001B[31m",
        "\u001B[32m",
        "\u001B[33m",
        "\u001B[34m",
        "\u001B[35m",
        "\u001B[36m",
        "\u001B[37m"
    };

    public String[] cssColors = {
            "black",
            "red",
            "green",
            "darkgoldenrod",
            "blue",
            "magenta",
            "cyan",
            "white"
        };
    
    private final String ANSI_RESET = "\u001B[0m";

    private List<GenericColumn> columns = new ArrayList<GenericColumn>();
    private List<GenericRow> rows = new ArrayList<GenericRow>();

    private String emptyTableText;

    public void addColumn(String header){
        addColumn(new GenericColumn(header));
    }

    public void addColumn(String header, String alignment){
        addColumn(new GenericColumn(header, alignment));
    }

    public void addColumn(GenericColumn column){
        columns.add(column);
    }

    public GenericColumn column(String header) {
    	GenericColumn res = new GenericColumn(header);
    	addColumn(res);
    	return res;
    }
    
    public GenericRow addRow() {
    	GenericRow row = new GenericRow();
    	rows.add(row);
    	return row;
    }
    
    /**
     * Use when all columns are a generic cell.
     * @param row
     */
    public void addRow(List<GenericCell> row){
    	GenericRow gr = new GenericRow();
    	gr.addGenericContent(row);
        rows.add(gr);
    }
    
    /**
     * Use when you're not sure all cells are generic cells. Those that aren't are converted
     * to generic cells.
     * @param row
     */
    public void addRowOfObjects(List<Object> row) {
    	if (row==null) return;
    	List<GenericCell> r = new ArrayList<GenericCell>();
    	for (Object o : row) {
    		if (o instanceof GenericCell) {
    			r.add((GenericCell)o);
    		} else {
    			r.add(new GenericCell(o));
    		}
    	}
    	GenericRow gr = new GenericRow();
    	gr.addGenericContent(r);
    	rows.add(gr);
    }
    
    public List<GenericColumn> getColumns() {
		return columns;
	}

    public boolean hasHeaders() {
    	if (columns!=null && columns.size()>0) {
    		for (GenericColumn c : columns) {
    			if (c.getHeader()!=null && c.getHeader().trim().length()>0) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public boolean isEmpty() {
    	return (rows==null || rows.size()==0);
    }
    
	public void setColumns(List<GenericColumn> columns) {
		this.columns = columns;
	}

	public List<GenericRow> getRows() {
		return rows;
	}

	public void setRows(List<GenericRow> rows) {
		this.rows = rows;
	}

	public String getEmptyTableText() {
        return emptyTableText;
    }

    public void setEmptyTableText(String emptyTableText) {
        this.emptyTableText = emptyTableText;
    }
    
    /**
     * Create a shell table from the provided table data.
     * @return
     */
    public ShellTable getShellTable(){
        ShellTable shellTable = new ShellTable();
        for(GenericColumn column : columns){
            Col col = shellTable.column(column.getHeader());
            if(column.getAlignment().equalsIgnoreCase(GenericColumn.ALIGNMENT_CENTER)){
                col.alignCenter();
            }
            else if(column.getAlignment().equalsIgnoreCase(GenericColumn.ALIGNMENT_RIGHT)){
                col.alignRight();
            }
        }
        
        for(GenericRow row : rows){
            Row r = shellTable.addRow();
            GenericCell gc;
            List<Object> rowContent = row.getContent();
            for(Object cell : rowContent){
            	if (cell instanceof GenericCell) {
            		gc = (GenericCell)cell;
	                if(gc.getColor() >= 0){
	                    String coloredData = String.format("%s%s%s", ansiColors[gc.getColor()], gc.toString(), ANSI_RESET);
	                    r.addContent(coloredData);
	                }else{
	                    r.addContent(gc);
	                }
            	} else {
            		r.addContent(cell);
            	}
            	
            }
        }

        shellTable.emptyTableText(emptyTableText);

        return shellTable;
    }
    
    /**
     * Create a HTML table from the provided data
     * @return
     */
    public HtmlTable getHtmlTable(){
        HtmlTable htmlTable = new HtmlTable();

        for(GenericColumn column : columns){
            if(column.getAlignment().equalsIgnoreCase(GenericColumn.ALIGNMENT_RIGHT)){
                htmlTable.column(column.getHeader(), GenericColumn.ALIGNMENT_RIGHT);
            }else{
                htmlTable.column(column.getHeader());
            }
        }
		
		if (rows.size()==0) {
			return htmlTable;
		}

		GenericCell gc;
		for (GenericRow row : rows) {
            List<Object> htmlRow = new ArrayList<Object>();

            for(Object cell : row.getContent()){
                String cellData = cell.toString();
                if (cell instanceof GenericCell) {
                	gc = (GenericCell)cell;
	                if(gc.getColor() >= 0){
	                    cellData = String.format("<span style='color:%s;'>%s</span>", cssColors[gc.getColor()], cell.toString());
	                }
                }
                htmlRow.add(cellData);
            }
            htmlTable.addRow(htmlRow);
		}
		
		return htmlTable;
    }
    
    
}
