/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.Intent

import Log._

class Main extends RActivity {
  val service = new LocalServiceConnection[MainService]

  onCreate {
    logger.info(s"Main: onCreate")

    setContentView{
      import org.scaloid.common.{SVerticalLayout, SButton}
      new SVerticalLayout {
        SButton("enable discovery").onClick {
          service(_.enableDiscovery(true)).get
        }
        SButton("disable discovery").onClick {
          service(_.enableDiscovery(false)).get
        }
        SButton("connect first").onClick {
          service(_.connectFirst()).get
        }
        SButton("stop all").onClick {
          stopServices()
          finish()
        }
      }
    }

    startServices()
  }

  private def startServices() {
    startService(new Intent(this, classOf[MainService]))
  }

  private def stopServices() {
    stopService(new Intent(this, classOf[MainService]))
  }
}

class MainService extends LocalService[MainService]
  with RflktService with LocusService
