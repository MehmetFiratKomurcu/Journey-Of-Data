package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

case class Address(country: String, city: String, street: String, buildingNumber: Int)
case class UserForm(name: String, password: String, age: Int, email: String, favSuperhero: String,
                    whyThisSuperhero: Option[String], address: Address)

object UserForm {
  object Preferences extends Enumeration {
    val Superman, Batman, MartianManhunter, WonderWoman = Value
  }

  val form: Form[UserForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText(minLength = 6),
      "age" -> number(min = 18, max = 65),
      "email" -> email,
      "favSuperhero" -> nonEmptyText,
      "whyThisSuperhero" -> optional(text),
      "address" -> mapping(
        "country" -> text,
        "city" -> text,
        "street" -> text,
        "buildingNumber" -> number
      )(Address.apply)(Address.unapply)
    )(UserForm.apply)(UserForm.unapply)
  )
}

object FormToJson {

  implicit val addressWrites = new Writes[Address] {
    override def writes(o: Address): JsValue = Json.obj(
      "country" -> o.country,
      "city" -> o.city,
      "street" -> o.street,
      "buildingNumber" -> o.buildingNumber
    )
  }

  implicit val userFormWrites = new Writes[UserForm] {
    override def writes(o: UserForm): JsValue = Json.obj(
      "name" -> o.name,
      "password" -> o.password,
      "age" -> o.age,
      "email" -> o.email,
      "favSuperhero" -> o.favSuperhero,
      "whyThisSuperhero" -> o.whyThisSuperhero,
      "address" -> o.address
    )
  }
  def formToJson(form: UserForm): JsValue = {
    println("formTOJSon func: " + form)
    println("formtojson func address : " + form.address.country)
    val userFormJson = UserForm(
      form.name, form.password, form.age, form.email,
      form.favSuperhero, form.whyThisSuperhero,
      Address(
        form.address.country, form.address.city, form.address.street, form.address.buildingNumber
      )
    )
    println("gönderilen json: " + userFormJson)
    val ww = Json.toJson(userFormJson)
    println(ww + "  xxxx")
    ww
  }
}
