package com.soulary.plantuml.generator.hibernate.example.person.employee;

import com.soulary.plantuml.generator.hibernate.example.person.Student;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.List;


@Entity
public class Professor extends Employee {
	@ManyToMany
	List<Student> students;
}
