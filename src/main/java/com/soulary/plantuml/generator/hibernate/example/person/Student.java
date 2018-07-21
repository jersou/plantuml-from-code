package com.soulary.plantuml.generator.hibernate.example.person;

import com.soulary.plantuml.generator.hibernate.example.person.employee.Professor;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.List;


@Entity
public class Student extends Person {
	@ManyToMany(mappedBy = "students")
	List<Professor> professors;
}
