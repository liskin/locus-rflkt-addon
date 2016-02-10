package cz.nomi.locusRflktAddon

import org.scaloid.common._

trait Log extends TagUtil {
  override implicit val loggerTag = LoggerTag("LocusRflktAddon")
}
