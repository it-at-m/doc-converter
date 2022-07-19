package de.muenchen.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class DDParserTest {

    final String DD_STRING =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<document>" +
                "<definition>" +
                    "<heading name=\"firstHeading\" get=\"default\"></heading>" +
                    "<paragraph name=\"firstParagraph\" weight=\"-1\" get=\"default\"></paragraph>" +
                    "<table></table>" +
                    "<table-row name=\"tableRows\" get=\"value1,value2,value3\" repeats=\"true\"></table-row>" +
                    "<table></table>" +
                "</definition>" +
                "<glossary>" +
                    "<heading><![CDATA[(\\d\\. .+)${blank-line}]]></heading>" +
                    "<paragraph><![CDATA[((?:.|\n|\r)+)${blank-line}]]></paragraph>" +
                    "<table><![CDATA[\\+-+\\+${new-line}]]></table>" +
                    "<table-row><![CDATA[\\|(?<value1>[0-9])\\|(?<value2>[0-9])\\|(?<value3>[0-9])\\|${new-line}]]></table-row>" +
                    "<blank-line><![CDATA[(?:${new-line}${new-line})]]></blank-line>" +
                    "<new-line><![CDATA[(?:\r?\n|\r)]]></new-line>" +
                "</glossary>" +
            "</document>";

    final String DOCUMENT_STRING =
            "1. Introduction\n" +
            "\n" +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua.\n" +
            "\n" +
            "+-----+\n" +
            "|1|2|3|\n" +
            "|4|5|6|\n" +
            "|7|8|9|\n" +
            "+-----+\n";

    final String TEMPLATE_STRING =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<document>" +
                "<h1 th:text=\"${greeting} + ', ' + ${reader} + '!'\"></h1>" +
                "<h3 th:text=\"${firstHeading.default}\"></h3>" +
                "<p th:text=\"${firstParagraph.default}\"></p>" +
                "<table>" +
                    "<tr th:each=\"row: ${tableRows}\">" +
                        "<td th:text=\"${row.value1}\"></td>" +
                        "<td th:text=\"${row.value2}\"></td>" +
                        "<td th:text=\"${row.value3}\"></td>" +
                    "</tr>" +
                "</table>" +
            "</document>";

    final Map<String, String> EXTRA_VALUES = Map.of("greeting", "Hello", "reader", "world");

    final String EXPECTED_OUTPUT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<document>" +
                "<h1>Hello, world!</h1>" +
                "<h3>1. Introduction</h3>" +
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
                    "eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>" +
                "<table>" +
                    "<tr>" +
                        "<td>1</td>" +
                        "<td>2</td>" +
                        "<td>3</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td>4</td>" +
                        "<td>5</td>" +
                        "<td>6</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td>7</td>" +
                        "<td>8</td>" +
                        "<td>9</td>" +
                    "</tr>" +
                "</table>" +
            "</document>";

    // TODO: Tests for unexpected cases.

    @Test
	void test() {
        final var dd = new ByteArrayInputStream(DD_STRING.getBytes(StandardCharsets.UTF_8));
        final var doc = new ByteArrayInputStream(DOCUMENT_STRING.getBytes(StandardCharsets.UTF_8));
        final var template = new ByteArrayInputStream(TEMPLATE_STRING.getBytes(StandardCharsets.UTF_8));

        try {
            final var sections = DDParser.parse(dd, doc);

            for (final var section : sections) {
                log.info("Parsed section '" + section.getName() + "': " + section.getContent());
            }

            final var output = DDParser.convert(template, sections, EXTRA_VALUES);
            assertThat(output, equalTo(EXPECTED_OUTPUT));
            log.info("Produced output matches expected output");
        } catch (IOException | DocumentException exception) {
            fail("Failed to parse or convert document: " + exception.getMessage());
        }
	}

}
