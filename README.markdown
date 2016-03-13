# Locus addon for Wahoo RFLKT

[![Gitter](https://badges.gitter.im/liskin/locus-rflkt-addon.svg)](https://gitter.im/liskin/locus-rflkt-addon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

An addon that enables [Wahoo RFLKT][rflkt] to be used with the awesome Android
multifunctional outdoor navigation app [Locus][locus]. It may be downloaded
either [here][releases] or via [Google Play][playstore]. The goal is to
support all of:

- ☑ clock
- ☑ current speed, heart rate, cadence
- ☑ track recording (start/pause/resume via button; show distance, average speed)
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
https://cloud.githubusercontent.com/assets/300342/13191955/07d7ea46-d768-11e5-93ac-fba1159539d2.jpg
) ![navigation](
https://cloud.githubusercontent.com/assets/300342/13191956/07d9238e-d768-11e5-8405-1127e4695b56.jpg
)

# Building

    $ GIT_ALLOW_PROTOCOL=file:git:http:https:ssh:hg git clone --recursive https://github.com/liskin/locus-rflkt-addon.git
    $ cd locus-rflkt-addon
    $ sbt
    > android:package
