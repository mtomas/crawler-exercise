package com.dcvc

import java.net.URL

import org.apache.commons.validator.routines.UrlValidator
import scala.collection.JavaConverters._


package object crawler {

  object URLValidator {
    val urlValidator = new UrlValidator()
    def validate(stringUrl: String) = urlValidator.isValid(stringUrl)
  }

  case class Links(links: List[URL])

}
