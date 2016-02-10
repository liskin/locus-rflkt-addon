package cz.nomi.locusRflktAddon

import org.scaloid.common._
import android.graphics.Color

import android.content.{BroadcastReceiver, Context, Intent}
import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.LocusConst
import locus.api.android.utils.LocusUtils.LocusVersion

class Main extends SActivity {
  lazy val meToo = new STextView("Me too")
  lazy val redBtn = new SButton(R.string.red)

  onCreate {
    contentView = new SVerticalLayout {
      style {
        case b: SButton => b.textColor(Color.RED).onClick(meToo.text = "PRESSED")
        case t: STextView => t textSize 10.dip
        case e: SEditText => e.backgroundColor(Color.YELLOW).textColor(Color.BLACK)
      }
      STextView("I am 10 dip tall")
      meToo.here
      STextView("I am 15 dip tall") textSize 15.dip // overriding
      new SLinearLayout {
        STextView("Button: ")
        redBtn.here
      }.wrap.here
      SEditText("Yellow input field fills the space").fill
    } padding 20.dip

    info(s"create")
  }

  broadcastReceiver(LocusConst.ACTION_PERIODIC_UPDATE) { (context: Context, intent: Intent) =>
    info(s"periodic update received")
    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      info(s"getGpsSatsAll: ${update.getGpsSatsAll}")
    }
  }

  private[this] implicit val loggerTag = LoggerTag("LocusRflktAddon")
}
