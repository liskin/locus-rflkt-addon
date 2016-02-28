/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity

import Log._

class Settings extends AppCompatActivity
  with RActivity with BackToParentActivity
{
  onCreate {
    logger.info("Settings: onCreate")

    setContentView {
      import macroid.FullDsl._

      getUi {
        f[SettingsFragment].framed(Gen.Id.settings, Gen.Tag.settings)
      }
    }
  }
}

class SettingsFragment extends PreferenceFragment {
}
