# Locus addon for Wahoo RFLKT

[![Gitter](https://badges.gitter.im/liskin/locus-rflkt-addon.svg)](https://gitter.im/liskin/locus-rflkt-addon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

An addon that enables [Wahoo RFLKT][rflkt] to be used with the awesome Android
multifunctional outdoor navigation app [Locus][locus]. It may be downloaded
either [here][releases] or via [Google Play][playstore]. The goal is to
support all of:

- ☑ clock
- ☑ current speed, heart rate, cadence, elevation
- ☑ track recording (start/pause/resume via button; show time, distance,
  average speed, meters climbed, etc.)
- ☑ navigation instructions
- ☑ SMS/call notifications
- ☐ an interval timer for training
- ☐ music player control

[rflkt]: http://eu.wahoofitness.com/devices/rflkt.html
[locus]: http://www.locusmap.eu/
[releases]: https://github.com/liskin/locus-rflkt-addon/releases
[playstore]: https://play.google.com/store/apps/details?id=cz.nomi.locusRflktAddon

# What it looks like?

![overview](
https://cloud.githubusercontent.com/assets/300342/13775985/e6b5a47c-eaa7-11e5-95e2-f13bad4aef10.jpg
) ![distance](
https://cloud.githubusercontent.com/assets/300342/13775986/e6d4ea30-eaa7-11e5-8f7a-ea8a634f1811.jpg
) ![elevation](
https://cloud.githubusercontent.com/assets/300342/13775987/e6f4dfca-eaa7-11e5-883d-aba393be6772.jpg
) ![navigation](
https://cloud.githubusercontent.com/assets/300342/13775991/e713f932-eaa7-11e5-8600-63d80401db94.jpg
)

# Building

    $ GIT_ALLOW_PROTOCOL=file:git:http:https:ssh:hg git clone --recursive https://github.com/liskin/locus-rflkt-addon.git
    $ cd locus-rflkt-addon
    $ sbt
    > android:package
