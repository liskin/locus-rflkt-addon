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
import android.net.Uri

import macroid._
import macroid.FullDsl._
import macroid.contrib.LpTweaks.matchWidth

import Log._
import Broadcasts._
import Const._

class Main extends AppCompatActivity with RActivity {
  private val service = new LocalServiceConnection[MainService]

  private var connectButton = slot[Button]
  private var disconnectButton = slot[Button]

  onCreate {
    logger.info(s"Main: onCreate")

    getSupportActionBar.setDisplayShowHomeEnabled(true)
    getSupportActionBar.setLogo(R.drawable.ic_launcher)
    getSupportActionBar.setDisplayUseLogoEnabled(true)

    setTitle(getString(R.string.main_label))

    setContentView {
      Ui.get {
        l[LinearLayout](
          w[Button] <~ matchWidth <~ wire(connectButton) <~ On.click { Ui {
            service(_.connectFirst()).get
          }},
          w[Button] <~ matchWidth <~ wire(disconnectButton) <~ On.click { Ui {
            service(_.disconnect()).get
          }} <~ text("disconnect")
        ) <~ vertical
      }
    }

    startService(mainServiceIntent); ()
  }

  service.onServiceConnected {
    refreshButton()
    checkLocus()
  }

  localBroadcastReceiver(localActionRefreshUi) { (context: Context, intent: Intent) =>
    refreshButton()
  }

  private def refreshButton() {
    Ui.run {
      if (service(_.isDisconnected()).getOrElse(true)) {
        Ui.sequence(
          service(_.describeFirst()).flatten match {
            case None =>
              connectButton <~ text("no device, use discovery") <~ disable
            case Some(d) =>
              connectButton <~ text(s"connect to $d") <~ enable
          },
          disconnectButton <~ disable
        )
      } else {
        val status = service(_.getStatus()).get
        Ui.sequence(
          connectButton <~ text(status) <~ disable,
          disconnectButton <~ enable
        )
      }
    }
  }

  private def checkLocus() {
    if (service(_.isLocusInstalled).get) {
      if (!service(_.isLocusPeriodicUpdatesEnabled).get) {
        val msg = "Periodic updates must be enabled in Settings → Miscellaneous."
        (dialog(s"$msg\n\nbring me to Locus Map") <~
          title("Periodic updates disabled") <~
          positiveOk(locusSettings) <~
          speak).run
      }
    } else {
      (dialog("bring me to Google Play") <~
        title("Locus Map not installed") <~
        positiveOk(locusGooglePlay) <~
        speak).run
    }
  }

  private def locusGooglePlay: Ui[_] = Ui {
    startActivity(new Intent(Intent.ACTION_VIEW,
      Uri.parse("market://details?id=menion.android.locus.pro")));
  }

  private def locusSettings: Ui[_] = Ui {
    service(_.launchLocus())
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
    notices.addNotice(new Notice("Locus API",
      "https://github.com/asamm/locus-api",
      "Copyright Asamm Software, s.r.o.", new GnuLesserGeneralPublicLicense3()))
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
  with RflktService with LocusService with NotificationService with BatteryService
{
  onRegister {
    logger.info(s"MainService: onCreate")
  }

  onUnregister {
    logger.info(s"MainService: onDestroy")
  }
}

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
