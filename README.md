<div id="top"></div>

<!-- PROJECT SHIELDS -->

<!-- END OF PROJECT SHIELDS -->

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="#">
    <img src="/images/logo.png" alt="Logo" height="200">
  </a>

<h3 align="center">doc-converter</h3>

  <p align="center">
    <i>Add a here a short description</i>
    <br /><a href="#">Report Bug</a>
    ·
    <a href="#">Request Feature</a>
  </p>
</div>

<!-- ABOUT THE PROJECT -->
## About The Project

doc-converter is a Java library for parsing a text document and then optionally converting into another format using the [Thymeleaf](https://github.com/thymeleaf/thymeleaf) template engine.

The document needs to be first described using a custom XML format called Document Definition (DD), more on that in "Documentation". Then the library is able to break the document up into a list of POJOs called "Sections".

After that, you can pass the list of sections to a wrapper function, along with a Thymeleaf template and a Map of additional variables, to convert it into the desired output format.

<p align="right">(<a href="#top">back to top</a>)</p>

### Built With

* Java 11
    * [dom4j](https://github.com/dom4j/dom4j)
    * [Thymeleaf](https://github.com/thymeleaf/thymeleaf)

<p align="right">(<a href="#top">back to top</a>)</p>

## Set up

doc-converter is currently only available on GitHub, so in order to add it your Maven/Gradle project dependencies, you need to use [JitPack](https://jitpack.io).

<p align="right">(<a href="#top">back to top</a>)</p>

## Documentation

For a usage example, see the [unit test](https://github.com/it-at-m/doc-converter/blob/dev/src/test/java/de/muenchen/converter/DDParserTest.java).

### Relevant Functions

```java
public static List<Section> parse(InputStream dd, InputStream doc) throws IOException, DocumentException {...}
```

```java
public static String convert(InputStream template, List<Section> sections, Map<String, String> extra) throws IOException {...}
```

### Relevant Classes

```java
public class Section {

	private final String name;

	private final Map<String, String> content;
	
	...

}
```


### Document Definition

Here is a sample document:

```
1. Introduction

Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
eiusmod tempor incididunt ut labore et dolore magna aliqua.

+-----+
|1|2|3|
|4|5|6|
|7|8|9|
+-----+
```

And here is a sample document definition:

```xml
<?xml version="1.0 encoding=UTF-8"?>
<document>
    <definition>
        <heading name="firstHeading" gets="default">
            <paragraph name="firstParagraph" gets="default"/>
        </heading>
        <table-top>
            <table-row name="tableRows" gets="value1,value2,value3" repeats="true"/>
            <table-bottom/>
        </table-top>
    </definition>
    <glossary>
        <heading><![CDATA[(\\d\\. .+)${blank-line}]]></heading>
        <paragraph><![CDATA[((?:.|\n|\r)+)${blank-line}]]></paragraph>
        <table-top><![CDATA[\\+-+\\+${new-line}]]></table-top>
        <table-bottom><![CDATA[\\+-+\\+]]></table-bottom>
        <table-row><![CDATA[\\|(?<value1>[0-9])\\|(?<value2>[0-9])\\|(?<value3>[0-9])\\|${new-line}]]></table-row>
        <blank-line><![CDATA[(?:${new-line}${new-line})]]></blank-line>
        <new-line><![CDATA[(?:\r?\n|\r)]]></new-line>
    </glossary>
</document>
```

<br/>

`document/glossary` is an obligatory element and defines a list of named regex patterns (Java dialect). They remotely resemble lexer rules, but unlike lexer rules, they are not applied before being specifically requested in the definition and do not represent the smallest unit. The smallest unit would be capturing groups, which are required are the parts of the document you may want to extract. If a glossary entry has multiple capturing groups, you need to name them. If it only has a single capturing group, its name can be left out. In that case, it will be set to "default". 
<br/>
Neither the names, nor the amount, nor the contents of the glossary entries are validated in any way before parsing. The parser simply "looks up" the required glossary entry as needed. 
<br/>
Enclosing the patterns in CDATA is recommended to avoid issues with escape characters in XML.

Additionally, you can use glossary entries within other glossary entries via the `${glossary-entry-name}` notation. Occurrences of this pattern are recursively replaced with the corresponding glossary entry before being turned into regex, meaning you don't have to worry about escape characters. However, self-referencing constructs, such as `<something>${something}\\D+</something>`, are not supported.

`document/definition` is the other obligatory element and defines where the patterns from the glossary entries are encountered and which capturing groups needs to be exposed.
The parser will attempt to match all these sections of the document in hierarchical order. Once a part of the document has been successfully matched, it is "claimed" and can't be matched again. The children of a section (e.g. the `<paragraph>` in `<heading>`) can only be matched against the text between the text matched by its parent and the text matched by the parent's next sibling (e.g. the text between "1. Introduction\n\n" and "+-----+\n").

The following attributes can be used to control the behavior of a section:
- `name`: The name that will be given to the section object. If this attribute is missing, the element name will be used.
- `gets`: Which capturing groups will be added to the content of the section object, separated by commas.
- `repeats`: Whether the section is supposed to appear >=1 times in a row. The parser will attempt to match the pattern as many times as possible and create a section object for each match.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please open an issue with the tag "enhancement", fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Open an issue with the tag "enhancement"
2. Fork the Project
3. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
4. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
5. Push to the Branch (`git push origin feature/AmazingFeature`)
6. Open a Pull Request

More about this in the [CODE_OF_CONDUCT](/CODE_OF_CONDUCT.md) file.

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` file for more information.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

it@m - opensource@muenchen.de





<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
