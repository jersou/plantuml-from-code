package com.soulary.plantuml.generator.hibernate.example;

import com.soulary.plantuml.generator.hibernate.example.person.Person;

import javax.persistence.Entity;


@Entity
public class Equipment {
	//@ManyToOne
	Person owner;
}
