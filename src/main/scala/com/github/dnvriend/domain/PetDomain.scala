package com.github.dnvriend.domain

object PetDomain {
  type FirstName = String
  type LastName = String
  type PetName = String
  type Age = Int
  type StreetName = String
  type HouseNumber = Int

  sealed trait Species
  case object Species {
    case object Cat extends Species
    case object Dog extends Species
  }

  sealed trait Gender
  case object Gender {
    case object Male extends Gender
    case object Female extends Gender
  }

  final case class Address(street: StreetName, houseNumber: HouseNumber)
  final case class Owner(firstName: FirstName, lastName: LastName, age: Age, address: Address)

  final case class Pet(name: PetName = "Fido", species: Species = Species.Dog, gender: Gender = Gender.Male, age: Age = 1, owner: Option[Owner] = Some(Owner("John", "Doe", 25, Address("first street", 10)))) {
    val GenderTypes = Gender
    val SpeciesTypes = Species
  }

  def withPet(f: Pet ⇒ Pet = identity[Pet])(g: Pet ⇒ Unit): Unit = (g compose f)(Pet())
}
