package org.notima.businessobjects.adapter.excel.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.excel.ExcelToInvoices;
import org.notima.businessobjects.adapter.excel.command.ConvertSpreadSheet;

public class TestConvertSpreadSheet {

    private ConvertSpreadSheet command;
    private ExcelToInvoices eti;

    @Before
    public void setUp() throws Exception {
        command = new ConvertSpreadSheet();
        Field etiField = ConvertSpreadSheet.class.getDeclaredField("eti");
        etiField.setAccessible(true);
        eti = (ExcelToInvoices) etiField.get(command);
    }

    private void setTaxExemptArticle(String value) throws Exception {
        Field field = ConvertSpreadSheet.class.getDeclaredField("taxExemptArticle");
        field.setAccessible(true);
        field.set(command, value);
    }

    private void callParseTaxExcemptArticles() throws Exception {
        Method method = ConvertSpreadSheet.class.getDeclaredMethod("parseTaxExcemptArticles");
        method.setAccessible(true);
        method.invoke(command);
    }

    @Test
    public void nullInputAddsNoArticles() throws Exception {
        setTaxExemptArticle(null);
        callParseTaxExcemptArticles();
        assertTrue(eti.getTaxExemptArticles().isEmpty());
    }

    @Test
    public void singleArticleIsAdded() throws Exception {
        setTaxExemptArticle("ART001");
        callParseTaxExcemptArticles();
        Set<String> articles = eti.getTaxExemptArticles();
        assertEquals(1, articles.size());
        assertTrue(articles.contains("ART001"));
    }

    @Test
    public void commaSeparatedArticlesAreAllAdded() throws Exception {
        setTaxExemptArticle("ART001,ART002,ART003");
        callParseTaxExcemptArticles();
        Set<String> articles = eti.getTaxExemptArticles();
        assertEquals(3, articles.size());
        assertTrue(articles.contains("ART001"));
        assertTrue(articles.contains("ART002"));
        assertTrue(articles.contains("ART003"));
    }

    @Test
    public void duplicateArticlesAreStoredOnce() throws Exception {
        setTaxExemptArticle("ART001,ART001,ART002");
        callParseTaxExcemptArticles();
        Set<String> articles = eti.getTaxExemptArticles();
        assertEquals(2, articles.size());
        assertTrue(articles.contains("ART001"));
        assertTrue(articles.contains("ART002"));
    }
}
