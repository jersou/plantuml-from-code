package com.soulary.plantuml.generator.hibernate.example.person;

import com.soulary.plantuml.generator.hibernate.example.Address;

import javax.persistence.Entity;
import java.util.List;


@Entity
public class Person {

	//	@OneToMany
	List<Address> adressList;

}
