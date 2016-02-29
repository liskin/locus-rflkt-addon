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

    onMenuClick(menu.add("License")) { mi => showLicense() }

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

  override def onBackPressed() {
    service(_.stopUnneeded())
    super.onBackPressed()
  }

  private def showLicense() {
    import de.psdev.licensesdialog.LicensesDialogFragment
    import de.psdev.licensesdialog.licenses._
    import de.psdev.licensesdialog.model._

    val notices = new Notices()
    notices.addNotice(new Notice(getString(R.string.app_name),
      "https://github.com/liskin/locus-rflkt-addon",
      "Copyright (C) 2016 Tomáš Janoušek", new OurLicense()))
    notices.addNotice(new Notice("LocusAPI",
      "https://bitbucket.org/asamm/locus-api/",
      "Copyright Asamm Software, s.r.o.", new GnuLesserGeneralPublicLicense3()))
    notices.addNotice(new Notice("LocusAddonPublicLib",
      "https://bitbucket.org/asamm/locus-api-android/",
      "Copyright Asamm Software, s.r.o.", new GnuGeneralPublicLicense30()))
    notices.addNotice(new Notice("macroid",
      "https://github.com/47deg/macroid",
      "Copyright Nick Stanchenko and contributors", new MITLicense()))
    notices.addNotice(new Notice("scaloid",
      "https://github.com/pocorall/scaloid",
      "Copyright 2014 Sung-Ho Lee and Scaloid contributors",
      new ApacheSoftwareLicense20()))
    notices.addNotice(new Notice("NoAnalytics",
      "https://github.com/mar-v-in/NoAnalytics",
      "Copyright 2012-2014 μg Project Team",
      new ApacheSoftwareLicense20()))

    new LicensesDialogFragment.Builder(this)
      .setNotices(notices)
      .setUseAppCompat(true)
      .build()
      .show(getSupportFragmentManager(), null)
  }

  private lazy val mainServiceIntent = new Intent(this, classOf[MainService])

  private lazy val settingsIntent = new Intent(this, classOf[Settings])
}

class MainService extends LocalService[MainService]
  with RflktService with LocusService

class OurLicense extends de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense30 {
  override def readSummaryTextFromResources(context: Context): String =
    """This program is free software; you can redistribute it and/or modify
      |it under the terms of the GNU General Public License as published by
      |the Free Software Foundation; either version 3 of the License, or
      |(at your option) any later version.
      |
      |This program is distributed in the hope that it will be useful, but
      |WITHOUT ANY WARRANTY; without even the implied warranty of
      |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
      |General Public License for more details.
      |
      |You should have received a copy of the GNU General Public License
      |along with this program; if not, see http://www.gnu.org/licenses.
      |
      |Additional permission under GNU GPL version 3 section 7
      |
      |If you modify this Program, or any covered work, by linking or
      |combining it with Wahoo Fitness Android API (or a modified version
      |of that library), containing parts covered by the terms of WAHOO
      |FITNESS, LLC SOFTWARE LICENSE AGREEMENT FOR WAHOO API
      |(http://api.wahoofitness.com/download/eula.html), the licensors of
      |this Program grant you additional permission to convey the resulting
      |work.""".stripMargin
}
