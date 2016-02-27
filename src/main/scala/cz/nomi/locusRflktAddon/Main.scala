/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.Intent

class Main extends SActivity {
  // move to top level once scaloid is gone
  import Log._

  val service = new LocalServiceConnection[MainService]

  onCreate {
    info(s"Main: onCreate")

    contentView = new SVerticalLayout {
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

    startServices()
  }

  private def startServices() {
    startService(new Intent(ctx, classOf[MainService]))
  }

  private def stopServices() {
    stopService(new Intent(ctx, classOf[MainService]))
  }
}

class MainService extends LocalService[MainService]
  with RflktService with LocusService
