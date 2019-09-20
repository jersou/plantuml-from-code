package com.soulary.plantuml.generator.hibernate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.util.Assert;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;

public class PlantUmlGenerator {

	private Reflections reflections = new Reflections(new MethodParameterScanner(),
			new SubTypesScanner()/* , new MemberUsageScanner() */);
	private LinkedHashMap<Class, Integer> classToProcess = new LinkedHashMap<>();
	private Set<Class> processedClass = new HashSet<>();
	private Predicate predicate;
	private PlantUmlGeneratorConfig config;

	public PlantUmlGenerator(PlantUmlGeneratorConfig config) {
		this.config = config;
		config.getEntrypointClassList().stream()
				.forEach(c -> classToProcess.put(c, config.getLimit()));
		predicate = createPredicate();
	}

	public Predicate createPredicate() {
		return new Predicate() {
			@Override
			public boolean test(Object o) {
				Class clazz = (Class) o;

				boolean customResult = true;
				if (config.getCustomPredicate() != null) {
					customResult = config.getCustomPredicate().test(clazz);
				}

				return customResult
						&& (config.getPackageToSelect().isEmpty()
								|| config.getPackageToSelect().stream()
										.anyMatch(p -> clazz.getName().startsWith(p)))
						&& !config.getPackageToIgnore().stream()
								.anyMatch(p -> clazz.getName().startsWith(p))
						&& !config.getWordsToIgnoreInClassName().stream()
								.anyMatch(clazz.getName()::contains)
						&& !config.getClassToIgnore().contains(clazz);
			}
		};
	}

	public String generate() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("@startuml\n");
		Map<String, LinkedList<String>> declarations = new HashMap<>();
		// init declarations
		declarations.put("", new LinkedList<String>());

		List<String> linksLines = new LinkedList<>();

		while (!classToProcess.isEmpty()) {

			Class clazz = classToProcess.keySet().iterator().next();

			if (clazz.getSimpleName().isEmpty()) {
				break;
			}

			Integer limit = classToProcess.get(clazz);
			classToProcess.remove(clazz);

			if (!processedClass.contains(clazz)) {
				processedClass.add(clazz);

				try {
					// Get namespace
					String namespace = getNamespace(clazz);

					// Get califier
					String califier = getCalifier(clazz);
					if (declarations.get(namespace) == null) {
						declarations.put(namespace, new LinkedList<String>());
					}

					declarations.get(namespace).add(califier);

					if ((limit == null || limit > 0)) {
						// Get inheritance
						processSubtypes(clazz, limit);

						if (config.isProcessMethodUsages()) {
							processMethodUsages(clazz, limit);
						}
					}

					linksLines.add(getExtends(clazz, limit));
					linksLines.addAll(getImplements(clazz, limit));
					if (config.isGenAssociation()) {
						linksLines.addAll(getAssoCompo(clazz, limit));
					}
				}
				catch (NoClassDefFoundError | IncompatibleClassChangeError ignore) {
				}
			}

		}

		declarations.forEach((namespace, lines) -> {
			if (!lines.isEmpty()) {

				String[] namespaceParts = namespace.split("\\.");

				final String indent = namespace == "" ? "" : "    ";
				if (namespace != "") {
					for (String part : namespaceParts) {
						stringBuilder.append("namespace " + part + " {\n");
					}
				}
				lines.forEach(line -> stringBuilder.append(
						indent + line + "\n"));
				if (namespace != "") {
					for (String part : namespaceParts) {
						stringBuilder.append("}\n");
					}
					stringBuilder.append("\n");
				}
			}
		});
		linksLines.forEach(link -> {
			if (link != null) {
				stringBuilder.append(link + "\n");
			}
		});

		stringBuilder.append("@enduml\n");
		return stringBuilder.toString();
	}

	private List<String> getAssoCompo(Class clazz, Integer limit) {
		List<String> lines = new LinkedList<>();
		for (Field field : clazz.getDeclaredFields()) {
			try {
				getAssoCompo(clazz, limit, lines, field);
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (config.getAssoCompoProcessor() != null) {
			config.getAssoCompoProcessor().addCustomAssoCompo(clazz, lines);
		}

		return lines;
	}

	private void getAssoCompo(Class clazz, Integer limit, List<String> lines, Field field)
			throws ClassNotFoundException {
		Class fieldClass = getTypeOfField(field);

		if (fieldClass != null && (hasCustomAnnot(field) || isNotIgnored(fieldClass))
				&& !fieldClass.getSimpleName().isEmpty()) {
			String assoType = null;
			boolean ignore = false;
			for (Annotation annotation : field.getDeclaredAnnotations()) {
				if (annotation.annotationType().equals(OneToOne.class)) {
					if (((OneToOne) annotation).mappedBy().isEmpty()) {
						assoType = "\"1\" --o \"1\"";
					}
					else {
						ignore = true;
					}
					break;
				}
				else if (annotation.annotationType().equals(ManyToOne.class)) {
					assoType = "\"1\" --o \"*\"";
					break;
				}
				else if (annotation.annotationType().equals(OneToMany.class)) {
					if (((OneToMany) annotation).mappedBy().isEmpty()) {
						assoType = "\"*\" --o \"1\"";
					}
					else {
						addClassToProcess(fieldClass, limit);
						ignore = true;
					}
					break;
				}
				else if (annotation.annotationType().equals(ManyToMany.class)) {
					if (((ManyToMany) annotation).mappedBy().isEmpty()) {
						assoType = "\"*\" --o \"*\"";
					}
					else {
						ignore = true;
					}
					break;
				} else if (config.getAssoCompoProcessor() != null &&
						config.getAssoCompoProcessor().addCustomAssoCompo(clazz, limit, lines, fieldClass, annotation)) {
					break;
				}
			}
			// pojo
			if (config.isProcessNonHibernateAnnotations() && assoType == null && !ignore
					&& !fieldClass.isPrimitive() && isNotIgnored(fieldClass)) {
				if (Collection.class.isAssignableFrom(field.getType().getClass())) {
					assoType = "\"*\" --o \"*\"";
				}
				else {
					assoType = "\"1\" --o \"*\"";
				}
			}

			if (assoType != null) {
				addClassToProcess(fieldClass, limit);
				if (limit == null || limit > 0 || processedClass.contains(fieldClass)
						|| classToProcess.containsKey(fieldClass)
						|| config.getEntrypointClassList().contains(fieldClass)) {
					lines.add(
							getUmlName(fieldClass) + " " + assoType + " "
									+ getUmlName(clazz)
									+ (config.isHideAssociationLabel() ? ""
											: (" : " + field.getName())));
				}
			}
		}
	}

	private String getNamespace(Class clazz) {
		if (config.isGenerateAutoNamespaces()) {
			System.out.println(clazz.getPackage().getName());
			return clazz.getPackage().getName();
		}
		else {
			AtomicReference<String> packagFound = new AtomicReference("");
			AtomicReference<String> namespaceFound = new AtomicReference("");

			config.getPackageNamespaceMap().forEach((packagePrefix, namespace) -> {
				if (clazz.getName().startsWith(packagePrefix)
						&& packagePrefix.length() > packagFound.get().length()) {
					packagFound.set(packagePrefix);
					namespaceFound.set(namespace);
				}
			});

			return namespaceFound.get();
		}
	}

	private String getCalifier(Class clazz) {
		StringBuilder califier = new StringBuilder();
		if (clazz.isEnum()) {
			califier.append("enum ");
		}
		else if (clazz.isInterface()) {
			califier.append("interface ");
		}
		else {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				califier.append("abstract ");
			}
			califier.append("class ");
		}
		califier.append(clazz.getSimpleName());

		return califier.toString();
	}

	private void processSubtypes(Class clazz, Integer limit) {
		try {
			reflections
					.getSubTypesOf(clazz)
					.stream()
					.forEach(c -> addClassToProcess((Class) c, limit));
		}
		catch (ReflectionsException ignore) {
		}
	}

	private void processMethodUsages(Class clazz, Integer limit) {
		try {
			reflections
					.getMethodsReturn(clazz)
					.stream().map(Method::getDeclaringClass)
					.forEach(c -> addClassToProcess((Class) c, limit));
			reflections
					.getMethodsMatchParams(clazz)
					.stream().map(Method::getDeclaringClass)
					.forEach(c -> addClassToProcess((Class) c, limit));
		}
		catch (ReflectionsException ignore) {
		}
	}

	private String getExtends(Class clazz, Integer limit) {
		Class superClass = clazz.getSuperclass();
		if (superClass != null && superClass != Object.class
				&& isNotIgnored(superClass)) {
			superClass.getSimpleName();
			addClassToProcess(superClass, limit);
			if (limit == null || limit > 0 || processedClass.contains(superClass)
					|| classToProcess.containsKey(superClass)) {
				return getUmlName(clazz) + " -up-|> " + getUmlName(superClass);
			}
		}
		return null;
	}

	private Collection<? extends String> getImplements(Class clazz, Integer limit) {
		List<String> lines = new LinkedList<>();
		for (Class interf : clazz.getInterfaces()) {
			if (isNotIgnored(interf)) {
				addClassToProcess(interf, limit);
				if (limit == null || limit > 0 || processedClass.contains(interf)
						|| classToProcess.containsKey(interf)) {
					lines.add(getUmlName(clazz) + " .up.|> " + getUmlName(interf));
				}
			}
		}
		return lines;
	}

	private boolean isNotIgnored(Class clazz) {
		return clazz != null && predicate.test(clazz);
	}

	private Class getTypeOfField(Field field) {
		if (Collection.class.isAssignableFrom(field.getType())) {
			try {
				return Class.forName(((ParameterizedType) field.getGenericType())
						.getActualTypeArguments()[0].getTypeName());
			}
			catch (ClassNotFoundException | ClassCastException e) {
				e.printStackTrace();
				return null;
			}
		}
		return field.getType();
	}

	private void addClassToProcess(Class clazz, Integer limit) {
		if (((limit == null || limit > 0) && isNotIgnored(clazz)
				&& !classToProcess.keySet().contains(clazz)) && clazz != null
				&& predicate.test(clazz)) {
			classToProcess.put(clazz, limit == null ? null : limit - 1);
		}
	}

	public String getUmlName(Class clazz) {
		String namespace = getNamespace(clazz);
		if (namespace != "") {
			return namespace + "." + clazz.getSimpleName();
		}
		return clazz.getSimpleName();
	}

	public boolean hasCustomAnnot(Field field) {
		if (config.getAssoCompoProcessor() != null) {
			return config.getAssoCompoProcessor().hasCustomAnnot(field);
		}
		return false;
	}

	public boolean fieldHasAnnot(Field field, Class annot) {
		return Arrays.stream(field.getAnnotations())
				.anyMatch(a -> a.annotationType().equals(annot));
	}

	public boolean classdHasAnnot(Class clazz, Class annot) {
		return Arrays.stream(clazz.getAnnotations())
				.anyMatch(a -> a.annotationType().equals(annot));
	}
}
