package de.muenchen.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDParserTest {

    // TODO: Lombok
    private static final Logger logger = LoggerFactory.getLogger(DDParserTest.class);

	// TODO: Tests for unexpected cases.

    @Test
	void test() {
        var ddStringBuilder = new StringBuilder();
        ddStringBuilder.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<document>" +
                "<definition>" +
                    "<heading name=\"firstHeading\" get=\"default\"></heading>" +
                    "<paragraph name=\"firstParagraph\" weight=\"-2\" get=\"default\"></paragraph>" +
                    "<table></table>" +
                    "<table-row name=\"tableData\" get=\"value1,value2,value3\"></table-row>" +
                    "<table></table>" +
                    "<heading name=\"secondHeading\" get=\"default\"></heading>" +
                    "<paragraph name=\"secondParagraph\" weight=\"-1\" get=\"default\"></paragraph>" +
                "</definition>" +
                "<glossary>" +
                    "<heading><![CDATA[(\\d\\. .+)${blank-line}]]></heading>" +
                    "<paragraph><![CDATA[((?:.|\n|\r)+)(?:${blank-line}|\\Z)]]></paragraph>" +
                    "<table><![CDATA[\\+-+\\+${new-line}]]></table>" +
                    "<table-row><![CDATA[\\|(?<value1>[0-9.,]+)\\|(?<value2>[0-9.,]+)\\|(?<value3>[0-9.,]+)\\|${new-line}]]></table-row>" +
                    "<blank-line><![CDATA[(?:${new-line}${new-line})]]></blank-line>" +
                    "<new-line><![CDATA[(?:\r?\n|\r)]]></new-line>" +
                "</glossary>" +
            "</document>"
        );
        var dd = new ByteArrayInputStream(ddStringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        var docStringBuilder = new StringBuilder();
        docStringBuilder.append(
            "1. Introduction\n" +
            "\n" +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua.\n" +
            "\n" +
            "+-----+\n" +
            "|1|2|3|\n" +
            "+-----+\n" +
            "\n" +
            "2. Conclusion\n" +
            "\n" +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco\n" +
            "laboris nisi ut aliquip ex ea commodo consequat."
        );
        var doc = new ByteArrayInputStream(docStringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        var templateStringBuilder = new StringBuilder();
        templateStringBuilder.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<document>" +
                "<id th:text=\"${id}\"></id>" +
                "<heading th:text=\"${firstHeading.default}\"></heading>" +
                "<p th:text=\"${firstParagraph.default}\"></p>" +
                "<table>" +
                    "<tr>" +
                        "<td th:text=\"${tableData.value1}\"></td>" +
                        "<td th:text=\"${tableData.value2}\"></td>" +
                        "<td th:text=\"${tableData.value3}\"></td>" +
                    "</tr>" +
                "</table>" +
                "<h1 th:text=\"${secondHeading.default}\"></h1>" +
                "<p th:text=\"${secondParagraph.default}\"></p>" +
            "</document>"
        );
        var template = new ByteArrayInputStream(templateStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        
        try {
            var sections = DDParser.parse(dd, doc);

            for (var section : sections) {
                logger.info(section.name + ": " + section.content);
            }

            var result = DDParser.convert(template, sections, Map.of("id", "0000"));
            logger.info(result);
        } catch (IOException | DocumentException exception) {
            // TODO: Fail test
            logger.error("Failed to parse document");
        }
	}

}
