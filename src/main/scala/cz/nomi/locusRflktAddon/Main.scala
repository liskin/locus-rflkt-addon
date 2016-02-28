/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Intent, Context}
import android.widget.LinearLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.{AppCompatButton => Button}
import android.view.ViewGroup.LayoutParams.{MATCH_PARENT, WRAP_CONTENT}
import android.view.MenuItem

import Log._
import Broadcasts._
import Const._

class Main extends AppCompatActivity with RActivity {
  private val service = new LocalServiceConnection[MainService]

  onCreate {
    logger.info(s"Main: onCreate")

    getSupportActionBar.setDisplayShowHomeEnabled(true)
    getSupportActionBar.setLogo(R.drawable.ic_launcher)
    getSupportActionBar.setDisplayUseLogoEnabled(true)

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

    onMenuClick {
      menu.add("preferences")
        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        .setIcon(android.R.drawable.ic_menu_preferences)
    } { mi =>
      startActivity(settingsIntent)
    }

    onMenuClick(menu.add("Quit")) { mi =>
      stopService(mainServiceIntent)
      finish()
    }
  }

  onPrepareOptionsMenu { menu =>
    menuItemDiscovery.setChecked(service(_.isDiscovering()).getOrElse(false))
  }

  broadcastReceiver(actionStop) { (context: Context, intent: Intent) =>
    logger.info(s"Main: actionStop received")
    finish()
  }

  private lazy val mainServiceIntent = new Intent(this, classOf[MainService])

  private lazy val settingsIntent = new Intent(this, classOf[Settings])
}

class MainService extends LocalService[MainService]
  with RflktService with LocusService
