package de.muenchen.converter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

// TODO: No magic strings
// TODO: Better naming
// TODO: Use Element instead of Node

@Slf4j
public class DDParser {
	
    private static final String DEFINITION_XPATH = "/document/definition";
    private static final String GLOSSARY_XPATH = "/document/glossary";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9-]+)}");

	public static List<Section> parse(InputStream dd, InputStream doc) throws IOException, DocumentException {
        var docAsString = new String(doc.readAllBytes(), StandardCharsets.UTF_8);
        var sections = new ArrayList<Section>();
        var reader = new SAXReader();
        var document = reader.read(dd);

        var glossary = document.selectSingleNode(GLOSSARY_XPATH);
        var definition = document.selectSingleNode(DEFINITION_XPATH);
        if (definition instanceof Element) {
            ((Element) definition).addAttribute("cache", docAsString);
        }

        var queue = new ConcurrentLinkedQueue<List<Node>>();
        queue.offer(definition.selectNodes("*"));

        while (!queue.isEmpty()) {
            var nodeList = queue.poll();
            Node previousSibling = null;

            for (var node : nodeList) {
                var glossaryEntry = getGlossaryEntry(node.getName(), glossary);

                String text;
                if (previousSibling != null) {
                    text = previousSibling.valueOf("@cache");
                } else {
                    text = node.getParent().valueOf("@cache");
                }

                var pattern = Pattern.compile(glossaryEntry);
                var matcher = pattern.matcher(text);

                while (matcher.find()) {
                    var nameAttribute = node.valueOf("@name");
                    var section = new Section(
                            nameAttribute.isBlank() ? node.getName() : nameAttribute,
                            new HashMap<>()
                    );

                    // TODO: Fail if a group does not exist or is empty
                    var captureGroups = node.valueOf("@gets");
                    if (!captureGroups.isBlank()) {
                        if (!captureGroups.equals("default")) {
                            // TODO: /, ?/
                            var captureGroupsArray = captureGroups.split(",");
                            for (var captureGroup : captureGroupsArray) {
                                section.getContent().put(captureGroup, matcher.group(captureGroup));
                            }
                        } else {
                            section.getContent().put("default", matcher.group(1));
                        }
                    }

                    var beforeMatch = text.substring(0, matcher.start());
                    if (previousSibling instanceof Element) {
                        ((Element) previousSibling).addAttribute("cache", beforeMatch);
                    }

                    var afterMatch = text.substring(matcher.end());
                    if (node instanceof Element) {
                        ((Element) node).addAttribute("cache", afterMatch);
                    }

                    sections.add(section);

                    if (!Boolean.parseBoolean(node.valueOf("@repeats"))) {
                        break;
                    }
                }

                previousSibling = node;
                queue.offer(node.selectNodes("*"));
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
            var currentValue = context.getVariable(section.getName());

            if (currentValue == null) {
                context.setVariable(section.getName(), section.getContent());
            } else {
                if (currentValue instanceof List) {
                    ((ArrayList<Map<String, String>>) currentValue).add(section.getContent());
                } else {
                    var list = new ArrayList<>();
                    list.add(currentValue);
                    list.add(section.getContent());
                    context.setVariable(section.getName(), list);
                }
            }
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
