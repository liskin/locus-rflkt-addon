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
    ButtonSettings.addToScreen(root)
    setPreferenceScreen(root)
  }
}

trait Settings2x2 {
  def title: String
  def prefix: String

  def entries: Seq[(String, String)]
  def northWestDef: String
  def northEastDef: String
  def southWestDef: String
  def southEastDef: String

  private lazy val northWest =
    ListPref(s"$prefix.northWest", "top left", entries, northWestDef)
  private lazy val northEast =
    ListPref(s"$prefix.northEast", "top right", entries, northEastDef)
  private lazy val southWest =
    ListPref(s"$prefix.southWest", "bottom left", entries, southWestDef)
  private lazy val southEast =
    ListPref(s"$prefix.southEast", "bottom right", entries, southEastDef)

  def addToScreen(root: PreferenceScreen)(implicit ctx: Context) {
    val cat = new PreferenceCategory(ctx)
    cat.setTitle(title)
    root.addPreference(cat)

    cat.addPreference(northWest.preference())
    cat.addPreference(northEast.preference())
    cat.addPreference(southWest.preference())
    cat.addPreference(southEast.preference())
  }

  def toDisplayConf()(implicit pref: SharedPreferences) =
    display.Pages.Conf2x2(
      northWest.preferenceVar()(),
      northEast.preferenceVar()(),
      southWest.preferenceVar()(),
      southEast.preferenceVar()()
    )
}

object ButtonSettings extends Settings2x2 {
  lazy val prefix = "allPages.buttons"
  lazy val title = "RFLKT button functions"

  lazy val entries = Seq(
    "Previous page" -> "PAGE_LEFT",
    "Next page" -> "PAGE_RIGHT",
    "Start/pause track recording" -> "START_STOP_WORKOUT",
    "Backlight for 5 seconds" -> "BACKLIGHT"
  )
  lazy val northWestDef = "START_STOP_WORKOUT"
  lazy val northEastDef = "BACKLIGHT"
  lazy val southWestDef = "PAGE_RIGHT"
  lazy val southEastDef = "PAGE_LEFT"
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
