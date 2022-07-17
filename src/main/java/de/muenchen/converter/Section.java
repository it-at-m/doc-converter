package de.muenchen.converter;

import lombok.Data;

import java.util.Map;

@Data
public class Section {

	private final String name;

	private final Map<String, String> content;

}
