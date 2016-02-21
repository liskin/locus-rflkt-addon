/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import org.scaloid.common._

trait Log extends TagUtil {
  override implicit val loggerTag = LoggerTag("LocusRflktAddon")
}
