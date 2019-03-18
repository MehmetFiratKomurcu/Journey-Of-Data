package controllers

import javax.inject._
import models.UserForm
import play.api.libs.json.JsValue
import play.api.mvc._
import kafka.kafkaProducer

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with play.api.i18n .I18nSupport {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action {implicit request =>
    Ok(views.html.index(UserForm.form))
  }

  def userFormPost() = Action {implicit request =>
    UserForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.index(formWithErrors))
      },
      formData => {
        dataToKafka(formData)
        println("this is here!!!")
        Ok(formData.toString)
      }
    )
  }


  def dataToKafka(formData: UserForm) = {
    //println("formData : ++++ " + formData)
    val formJson: JsValue = models.FormToJson.formToJson(formData)
    println("--- " + formJson + " ---")
    val topic: String = "test"
    val brokers: String = "localhost:9092"
    val kafkaProducer = new kafkaProducer(topic, brokers, formJson)
    kafkaProducer.run()
  }

}
