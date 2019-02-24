
package io.github.ejif.chromej;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Generates a Java interface for each Chrome Devtools domain. Each command in the domain
 * corresponds to a method in the interface.
 */
@RequiredArgsConstructor
public final class ProtocolGenerator {

    private static final Logger log = LoggerFactory.getLogger(ProtocolGenerator.class);

    private final URI protocolUrl;
    private final File rootFile;
    private final String outputPackage;

    public void generate() {
        Protocol protocol = getProtocol();

        TypeSpec.Builder spec = TypeSpec.interfaceBuilder("WsProtocol")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(AutoCloseable.class);
        for (Domain domain : protocol.domains) {
            generateDomain(domain);
            spec.addMethod(MethodSpec.methodBuilder("get" + domain.domain)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(getPackage(domain.domain), domain.domain))
                .build());
        }
        write(outputPackage, spec.build());
    }

    private Protocol getProtocol() {
        try {
            String protocolJson = IOUtils.toString(protocolUrl, StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(protocolJson, Protocol.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a class for all types declared in this domain, a class for all request and response
     * objects, and an interface with a method for each declared command.
     *
     * @param domain
     */
    private void generateDomain(Domain domain) {
        String package_ = getPackage(domain.domain);

        TypeSpec.Builder spec = TypeSpec.interfaceBuilder(domain.domain)
            .addModifiers(Modifier.PUBLIC);
        if (domain.description != null)
            spec.addJavadoc("$L\n", domain.description);

        if (domain.types != null)
            for (Type type : domain.types)
                generateClass(domain.domain, type.id, type.description, type.type, type.properties, type.items, type.enum_);

        if (domain.commands != null)
            for (Command command : domain.commands) {
                MethodSpec.Builder method = MethodSpec.methodBuilder(command.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
                if (command.description != null)
                    method.addJavadoc("$L\n", command.description);
                if (command.parameters != null) {
                    String simpleName = WordUtils.capitalize(command.name) + "Request";
                    method.addParameter(ClassName.get(package_, simpleName), "request");
                    generateClass(domain.domain, simpleName, null, null, command.parameters, null, null);
                }
                if (command.returns != null) {
                    String simpleName = WordUtils.capitalize(command.name) + "Response";
                    method.returns(ClassName.get(package_, simpleName));
                    generateClass(domain.domain, simpleName, null, null, command.returns, null, null);
                }
                spec.addMethod(method.build());
            }

        write(package_, spec.build());
    }

    /**
     * Generates a class file in the package for the given domain. One of <code>type</code>,
     * <code>fields</code>, <code>items</code>, or <code>enum_</code> must be present.
     *
     * @param domain
     *            The class will be generated in the {outputPackage}.{domain} package
     * @param simpleName
     *            The simple name of the class
     * @param description
     *            The documentation for the class
     * @param type
     *            If none of the remaining fields are set, the generated class will be an alias of
     *            this specified type; for example, if type="string", then the class will contain a
     *            single String field annotated with @JsonValue
     * @param fields
     *            If set, the generated class will be a standard POJO with the given fields
     * @param items
     *            If set, the generated class will be a <code>List&lt;InnerType&gt;</code>
     * @param enum_
     *            If set, the generated class will be an enum with the given values
     */
    private void generateClass(String domain, String simpleName, String description, String type, List<Field> fields, Items items,
            List<String> enum_) {
        TypeSpec.Builder spec = enum_ == null
                ? TypeSpec.classBuilder(simpleName).addModifiers(Modifier.FINAL)
                : TypeSpec.enumBuilder(simpleName);
        spec.addModifiers(Modifier.PUBLIC);
        if (description != null)
            spec.addJavadoc("$L\n", description);

        if (fields != null) {
            spec.addAnnotation(AllArgsConstructor.class);
            spec.addAnnotation(Builder.class);
            spec.addAnnotation(AnnotationSpec.builder(Data.class)
                .addMember("staticConstructor", "$S", "of")
                .build());
            spec.addAnnotation(AnnotationSpec.builder(JsonInclude.class)
                .addMember("value", "$T.$L", Include.class, Include.NON_NULL)
                .build());
            for (Field field : fields)
                spec.addField(toFieldSpec(domain, field, simpleName));
        } else if (items != null) {
            TypeName typeName = toTypeName(domain, items.type, items.ref);
            spec.superclass(ParameterizedTypeName.get(
                ClassName.get(ArrayList.class),
                typeName.isPrimitive() ? typeName.box() : typeName));
            spec.addField(FieldSpec.builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("1")
                .build());
        } else if (enum_ != null) {
            for (String val : enum_) {
                spec.addEnumConstant(getEnumName(val), TypeSpec.anonymousClassBuilder("")
                    .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                        .addMember("value", "$S", val)
                        .build())
                    .build());
            }
        } else if (type != null) {
            TypeName typeName = toTypeName(domain, type, null);
            spec.addAnnotation(Data.class);
            spec.addField(FieldSpec.builder(typeName, "value")
                .addAnnotation(JsonValue.class)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());
            ClassName className = ClassName.get(getPackage(domain), simpleName);
            spec.addMethod(MethodSpec.methodBuilder("of")
                .addAnnotation(JsonCreator.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(typeName, "value")
                .returns(className)
                .addCode("return new $T(value);\n", className)
                .build());
        }

        write(getPackage(domain), spec.build());
    }

    /**
     * Converts a field to a JavaPoet FieldSpec.
     *
     * @param domain
     *            The domain that the outer class is in; used to resolve references to declared
     *            types in the same domain
     * @param field
     *            The field to convert into a spec
     * @param outerClassSimpleName
     *            The simple name of the class containing this field, used only to namespace
     *            generated enums (because Java doesn't support anonymous enums)
     * @return The field spec that can be added to a type spec
     */
    private FieldSpec toFieldSpec(String domain, Field field, String outerClassSimpleName) {
        TypeName typeName;
        if (field.items != null) {
            TypeName innerTypeName = toTypeName(domain, field.items.type, field.items.ref);
            typeName = ParameterizedTypeName.get(
                ClassName.get(List.class),
                innerTypeName.isPrimitive() ? innerTypeName.box() : innerTypeName);
        } else if (field.enum_ != null) {
            String enumName = outerClassSimpleName + WordUtils.capitalize(field.name);
            typeName = ClassName.get(getPackage(domain), enumName);
            generateClass(domain, enumName, null, null, null, null, field.enum_);
        } else {
            typeName = toTypeName(domain, field.type, field.ref);
        }

        String fieldName = field.name;
        while (SourceVersion.isKeyword(fieldName))
            fieldName += '_';

        FieldSpec.Builder field_ = FieldSpec.builder(typeName, fieldName)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL);
        if (field.description != null)
            field_.addJavadoc("$L\n", field.description);
        return field_.build();
    }

    /**
     * Converts a type or ref to a JavaPoet TypeName. One of <code>type</code> or <code>ref</code>
     * must be present.
     *
     * @param domain
     *            The domain that this type is being referenced in; used to resolve the ref
     * @param type
     *            A string corresponding to a primitive JSON type
     * @param ref
     *            A relative reference to a type declared in this domain, or an absolute reference
     *            of the form {domain}.{typeName}
     * @return The TypeName
     */
    private TypeName toTypeName(String domain, String type, String ref) {
        if (type != null) {
            switch (type) {
                case "any":
                    return TypeName.OBJECT;
                case "boolean":
                    return TypeName.BOOLEAN;
                case "integer":
                    return TypeName.INT;
                case "number":
                    return TypeName.DOUBLE;
                case "object":
                    return TypeName.OBJECT;
                case "string":
                    return TypeName.get(String.class);
                default:
                    throw new IllegalStateException("Invalid type: " + type);
            }
        } else if (ref != null) {
            String[] parts = ref.split("\\.");
            if (ref.contains("."))
                return ClassName.get(getPackage(parts[0]), parts[1]);
            else
                return ClassName.get(getPackage(domain), parts[0]);
        } else {
            throw new IllegalStateException("One of 'type' and 'ref' must be defined");
        }
    }

    private String getPackage(String domain) {
        return outputPackage + "." + domain.toLowerCase();
    }

    private void write(String package_, TypeSpec spec) {
        try {
            JavaFile.builder(package_, spec)
                .build()
                .writeTo(rootFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a string to an enum identifier following Java conventions (capital letters separated
     * by underscores).
     *
     * @param val
     *            The string to convert
     * @return An identifier following naming conventions for enums
     */
    private static String getEnumName(String val) {
        StringBuilder enumName = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            if (i > 0 && Character.isUpperCase(val.charAt(i)))
                if (Character.isLowerCase(val.charAt(i - 1))
                        || (i < val.length() - 1 && Character.isLowerCase(val.charAt(i + 1)))) {
                    enumName.append('_');
                }
            if (Character.isLetter(val.charAt(i)))
                enumName.append(Character.toUpperCase(val.charAt(i)));
            else
                enumName.append('_');
        }
        return enumName.toString();
    }

    @Data
    private static class Protocol {
        final List<Domain> domains;
    }

    @Data
    private static class Domain {
        final String domain;
        String description;
        boolean deprecated;
        boolean experimental;
        List<String> dependencies;
        List<Type> types;
        List<Command> commands;
        List<Event> events;
    }

    @Data
    private static class Type {
        final String id;
        String description;
        boolean experimental;
        final String type;
        List<Field> properties;
        Items items;
        @JsonProperty("enum")
        List<String> enum_;
    }

    @Data
    private static class Command {
        final String name;
        String description;
        boolean deprecated;
        boolean experimental;
        List<Field> parameters;
        List<Field> returns;
        String redirect;
    }

    @Data
    private static class Event {
        final String name;
        String description;
        boolean deprecated;
        boolean experimental;
        List<Field> parameters;
    }

    @Data
    private static class Field {
        final String name;
        String description;
        boolean deprecated;
        boolean experimental;
        boolean optional;
        String type;
        @JsonProperty("$ref")
        String ref;
        Items items;
        @JsonProperty("enum")
        List<String> enum_;
    }

    @Data
    private static class Items {
        String type;
        @JsonProperty("$ref")
        String ref;
    }

    public static void main(String[] args) {
        log.info("Generating websocket protocol...");
        new ProtocolGenerator(
            URI.create("https://raw.githubusercontent.com/ChromeDevTools/debugger-protocol-viewer/master/_data/tot/protocol.json"),
            new File("../ws-protocol/src/main/java"),
            "io.github.ejif.chromej.protocol")
                .generate();
        log.info("Done generating websocket protocol.");
    }
}
