package com.soulary.plantuml.generator.hibernate;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Predicate;

@Getter
@Setter
public class PlantUmlGeneratorConfig {
	private Predicate customPredicate;
	private boolean hideAssociationLabel = true;
	private boolean genAssociation = true;
	private boolean generateAutoNamespaces = false;
	private boolean processMethodUsages = false;
	private Set<Class> entrypointClassList = new HashSet<>();
	private Map<String, String> packageNamespaceMap = new HashMap<>();
	private Map<String, String> serviceToNamespaceMap = new HashMap<>();
	private List<String> wordsToIgnoreInClassName = new ArrayList<>();
	private List<Class> classToIgnore = new ArrayList<>();
	private List<String> packageToIgnore = new ArrayList<>();
	private List<String> packageToSelect = new ArrayList<>();
	private Integer limit;
	private boolean processNonHibernateAnnotations = false;

	public void addEntrypointClass(Class... classArray) {
		entrypointClassList.addAll(Arrays.asList(classArray));
	}

	public void addPackageNamespaceMapping(String... mappings) {
		for (String mapping : mappings) {
			String[] parts = mapping.split("->");
			packageNamespaceMap.put(parts[0].trim(), parts[1].trim());
		}
	}

	public void addServiceToNamespaceMapping(String... mappings) {
		for (String mapping : mappings) {
			String[] parts = mapping.split("->");
			serviceToNamespaceMap.put(parts[0].trim(), parts[1].trim());
		}
	}

	public void addWordsToIgnoreInClassName(String... toIgnore) {
		wordsToIgnoreInClassName.addAll(Arrays.asList(toIgnore));
	}

	public void addPackageToIgnore(String... toIgnore) {
		packageToIgnore.addAll(Arrays.asList(toIgnore));
	}

	public void addClassToIgnore(Class... classArray) {
		classToIgnore.addAll(Arrays.asList(classArray));
	}

	public void addPackageToSelect(String... toSelect) {
		packageToSelect.addAll(Arrays.asList(toSelect));
	}

}
