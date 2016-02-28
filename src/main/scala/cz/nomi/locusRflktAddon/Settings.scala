/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Context, SharedPreferences}
import android.preference._
import android.support.v7.app.AppCompatActivity
import android.widget.{LinearLayout, TextView}

import Log._

class Settings extends AppCompatActivity
  with RActivity with BackToParentActivity
{
  import macroid.Tweak

  onCreate {
    logger.info("Settings: onCreate")

    setContentView {
      import scala.language.postfixOps
      import macroid.FullDsl._
      import macroid.contrib.LpTweaks.matchWidth

      getUi {
        l[LinearLayout](
          w[TextView] <~ text("(need reconnect to take effect)") <~ matchWidth <~ center <~ padding(all = 3 dp),
          f[SettingsFragment].framed(Gen.Id.settings, Gen.Tag.settings)
        ) <~ vertical
      }
    }
  }

  private def center: Tweak[TextView] = {
    import android.view.Gravity
    Tweak[TextView](_.setGravity(Gravity.CENTER_HORIZONTAL))
  }
}

class SettingsFragment extends PreferenceFragment with RFragment {
  onCreate {
    implicit val ctx: Context = getActivity()
    val root = getPreferenceManager().createPreferenceScreen(ctx)

    val buttons = new PreferenceCategory(ctx)
    buttons.setTitle("RFLKT button functions")
    root.addPreference(buttons)

    import ButtonSettings._
    buttons.addPreference(northEast.preference())
    buttons.addPreference(northWest.preference())
    buttons.addPreference(southEast.preference())
    buttons.addPreference(southWest.preference())

    setPreferenceScreen(root)
  }
}

object ButtonSettings {
  private val entries = Seq(
    "Previous page" -> "PAGE_LEFT",
    "Next page" -> "PAGE_RIGHT",
    "Start/pause track recording" -> "START_STOP_WORKOUT",
    "Backlight for 5 seconds" -> "BACKLIGHT"
  )

  val northEast = ListPref("allPages.buttons.northEast",
    "top left", entries, "BACKLIGHT")
  val northWest = ListPref("allPages.buttons.northWest",
    "top right", entries, "START_STOP_WORKOUT")
  val southEast = ListPref("allPages.buttons.southEast",
    "bottom left", entries, "PAGE_LEFT")
  val southWest = ListPref("allPages.buttons.southWest",
    "bottom right", entries, "PAGE_RIGHT")

  import com.wahoofitness.common.{display => w}
  import w.DisplayButtonPosition._

  def toDisplayConf()(implicit pref: SharedPreferences) =
    Seq[(w.DisplayButtonPosition, String)](
      (NORTH_EAST, northEast.preferenceVar()()),
      (NORTH_WEST, northWest.preferenceVar()()),
      (SOUTH_EAST, southEast.preferenceVar()()),
      (SOUTH_WEST, southWest.preferenceVar()())
    )
}

abstract class PreferenceBuilder[T] {
  def preference()(implicit ctx: Context): Preference
  def preferenceVar(): PreferenceVar[T]
}

case class ListPref(key: String, title: String,
    entries: Seq[(String, String)], default: String)
  extends PreferenceBuilder[String]
{
  def preference()(implicit ctx: Context): Preference =
    new ListPreference(ctx) {
      setKey(key)
      setTitle(title)
      setSummary("%s")
      setEntries(entries.map(_._1: CharSequence).toArray)
      setEntryValues(entries.map(_._2: CharSequence).toArray)
      setDefaultValue(default)
    }

  def preferenceVar(): PreferenceVar[String] =
    Preferences.preferenceVar(key, default)
}
