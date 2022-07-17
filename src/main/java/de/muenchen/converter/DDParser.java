package de.muenchen.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

// TODO: Lombok
// TODO: Magic strings

public class DDParser {
	
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9-]+)\\}");

	private static final Logger logger = LoggerFactory.getLogger(DDParser.class);

	public static List<Section> parse(InputStream dd, InputStream doc) throws IOException, DocumentException {
        var docAsString = new String(doc.readAllBytes(), StandardCharsets.UTF_8);
        var sections = new ArrayList<Section>();
        var reader = new SAXReader();
        var document = reader.read(dd);

        var glossary = document.selectSingleNode("/document/glossary");

        
        var nodes = document.selectNodes("/document/definition/*");
        
        Collections.sort(nodes, (a, b) -> {
            var aWeightAttribute = a.numberValueOf("@weight");
            var bWeightAttribute = b.numberValueOf("@weight");

            var aWeight = aWeightAttribute == null ? 0 : aWeightAttribute;
            var bWeight = bWeightAttribute == null ? 0 : bWeightAttribute;

            return -Integer.compare(aWeight.intValue(), bWeight.intValue());
        });

        // TODO: Once sections can be nested: confirm that content is not text
        for (var node : nodes) {
            var glossaryEntry = getGlossaryEntry(node.getName(), glossary);
            var pattern = Pattern.compile(glossaryEntry);
            var matcher = pattern.matcher(docAsString);

            if (matcher.find()) {
                var section = new Section();
                var nameAttribute = node.valueOf("@name");
                section.name = nameAttribute.isBlank() ? node.getName() : nameAttribute;
                section.content = new HashMap<String, String>();

                // TODO: Fail if a group does not exist or is empty
                var captureGroups = node.valueOf("@get");
                if (!captureGroups.isBlank()) {
                    if (!captureGroups.equals("default")) {
                        // TODO: /, ?/
                        var captureGroupsArray = captureGroups.split(",");
                        for (var captureGroup : captureGroupsArray) {
                            section.content.put(captureGroup, matcher.group(captureGroup));
                        }
                    } else {
                        section.content.put("default", matcher.group(1));
                    }
                }

                var beforeMatch = docAsString.substring(0, matcher.start());
                var afterMatch = docAsString.substring(matcher.end(), docAsString.length());
                docAsString = beforeMatch + afterMatch;

                sections.add(section);
            }
        }

        return sections;
    }

    // TODO: Parameter for locale (or remove locale)
    // TODO: Parameter for template mode?
    public static String convert(InputStream template, List<Section> sections, Map<String, String> extra) throws IOException {
        var templateAsString = new String(template.readAllBytes(), StandardCharsets.UTF_8);

        var templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.XML);

        var templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        var context = new Context(Locale.getDefault());
        
        for (var section : sections) {
            context.setVariable(section.name, section.content);
        }

        for (var ext : extra.entrySet()) {
            context.setVariable(ext.getKey(), ext.getValue());
        }

        return templateEngine.process(templateAsString, context);
    }

    // TODO: Catch non-existent entries 
    private static String getGlossaryEntry(String entryName, Node glossary) {
        var entry = glossary.selectSingleNode(entryName).getText();
        entry = formatRegex(entry, glossary);
        return entry;
    }

    private static String formatRegex(String regex, Node glossary) {
        var matcher = VARIABLE_PATTERN.matcher(regex);

        while (matcher.find()) {
            var glossaryEntry = getGlossaryEntry(matcher.group(1), glossary);
            regex = regex.replace(matcher.group(0), glossaryEntry);
        }

        return regex;
    }

}
