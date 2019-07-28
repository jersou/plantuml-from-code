package com.soulary.plantuml.generator.hibernate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

public interface IAssoCompoProcessor {
	void addCustomAssoCompo(Class clazz, List<String> lines);
	boolean addCustomAssoCompo(Class clazz, Integer limit, List<String> lines, Class fieldClass, Annotation annotation);
	boolean hasCustomAnnot(Field field);
}
