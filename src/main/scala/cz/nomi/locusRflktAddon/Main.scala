/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.Intent
import android.widget.LinearLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.{AppCompatButton => Button}
import android.view.ViewGroup.LayoutParams.{MATCH_PARENT, WRAP_CONTENT}
import android.view.MenuItem

import Log._

class Main extends AppCompatActivity with RActivity {
  private val service = new LocalServiceConnection[MainService]

  onCreate {
    logger.info(s"Main: onCreate")

    setContentView {
      import macroid._
      import macroid.FullDsl._
      import macroid.contrib.LpTweaks.matchWidth

      getUi {
        l[LinearLayout](
          w[Button] <~ text("connect first") <~ matchWidth <~ On.click { Ui {
            service(_.connectFirst()).get
          }}
        ) <~ vertical
      }
    }

    val _ = startService(mainServiceIntent)
  }

  private var menuItemDiscovery: MenuItem = _

  onCreateOptionsMenu { menu =>
    menuItemDiscovery =
      onMenuClick(menu.add("Discovery").setCheckable(true)) { mi =>
        service { s =>
          s.enableDiscovery(!s.isDiscovering())
        }.get
      }

    onMenuClick(menu.add("Quit")) { mi =>
      stopService(mainServiceIntent)
      finish()
    }
  }

  onPrepareOptionsMenu { menu =>
    menuItemDiscovery.setChecked(service(_.isDiscovering()).getOrElse(false))
  }

  private lazy val mainServiceIntent = new Intent(this, classOf[MainService])
}

class MainService extends LocalService[MainService]
  with RflktService with LocusService
