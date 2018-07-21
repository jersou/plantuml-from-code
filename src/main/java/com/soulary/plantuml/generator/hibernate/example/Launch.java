package com.soulary.plantuml.generator.hibernate.example;

import com.soulary.plantuml.generator.hibernate.PlantUmlGenerator;
import com.soulary.plantuml.generator.hibernate.PlantUmlGeneratorConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Launch {

	public static void main(String[] args) throws IOException {
		PlantUmlGeneratorConfig config = new PlantUmlGeneratorConfig();
		config.addEntrypointClass(Equipment.class);
		config.setProcessNonHibernateAnnotations(true);
		String plantUml = new PlantUmlGenerator(config).generate();
		Files.write(Paths.get("output.plantuml"), plantUml.getBytes());
	}
}
